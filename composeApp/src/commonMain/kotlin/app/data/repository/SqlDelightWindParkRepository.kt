package app.data.repository

import app.core.model.WindPark
import app.core.model.Metric
import app.core.model.WindTurbine
import app.data.local.dao.*
import app.data.local.entity.WindParkEntity
import app.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SqlDelightWindParkRepository(
    private val database: AppDatabase,
) : WindParkRepository {
    private val windParkDao: WindParkDao = SqlDelightWindParkDao(database)
    private val windTurbineDao: WindTurbineDao = SqlDelightWindTurbineDao(database)
    private val metricDao: MetricDao = SqlDelightMetricDao(database)
    private val favoriteDao: FavoriteDao = SqlDelightFavoriteDao(database)
    private val recentWindParkDao: RecentWindParkDao = SqlDelightRecentWindParkDao(database)
    private val dataHintDao: DataHintDao = SqlDelightDataHintDao(database)
    private val snapshotMetadataDao: SnapshotMetadataDao = SqlDelightSnapshotMetadataDao(database)

    override suspend fun getWindParks(): List<WindPark> = withContext(Dispatchers.IO) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        windParkDao.getAll().map { it.toDomain(favorites.contains(it.id)) }
    }

    override suspend fun getWindPark(id: String): WindPark? = withContext(Dispatchers.IO) {
        val entity = windParkDao.getById(id) ?: return@withContext null
        val isFav = favoriteDao.isFavorite(id)
        entity.toDomain(isFav)
    }

    override suspend fun searchWindParks(query: String): List<WindPark> = withContext(Dispatchers.IO) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        windParkDao.search(query).map { it.toDomain(favorites.contains(it.id)) }
    }

    override suspend fun getFavoriteWindParks(): List<WindPark> = withContext(Dispatchers.IO) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        windParkDao.getAll()
            .filter { favorites.contains(it.id) }
            .map { it.toDomain(true) }
    }

    override suspend fun isFavorite(parkId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(parkId)
    }

    override suspend fun setFavorite(parkId: String, isFavorite: Boolean): Unit = withContext(Dispatchers.IO) {
        if (isFavorite) {
            favoriteDao.addFavorite(parkId, epochMillis())
        } else {
            favoriteDao.removeFavorite(parkId)
        }
    }

    override suspend fun getRecentWindParks(limit: Long): List<WindPark> = withContext(Dispatchers.IO) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val recentIds = recentWindParkDao.getRecentWindParkIds(limit)
        val entitiesMap = windParkDao.getAll().filter { recentIds.contains(it.id) }.associateBy { it.id }
        recentIds.mapNotNull { id ->
            entitiesMap[id]?.toDomain(favorites.contains(id))
        }
    }

    override suspend fun recordRecentWindPark(parkId: String): Unit = withContext(Dispatchers.IO) {
        recentWindParkDao.recordRecentWindPark(parkId, epochMillis())
    }

    override suspend fun clearRecentWindParks(): Unit = withContext(Dispatchers.IO) {
        recentWindParkDao.clear()
    }

    override suspend fun getMetricsForPark(parkId: String): List<Metric> = withContext(Dispatchers.IO) {
        metricDao.getForSubject("wind_park", parkId)
    }

    override suspend fun getMetricsForNational(): List<Metric> = withContext(Dispatchers.IO) {
        val allMetrics = metricDao.getAll()
        val groups = allMetrics.groupBy { it.metricType }
        groups.map { (type, list) ->
            val sum = list.sumOf { it.value ?: 0.0 }
            Metric(
                id = "national_$type",
                subjectType = "national",
                subjectId = "DE",
                metricType = type,
                value = sum,
                unit = list.firstOrNull()?.unit ?: "",
                period = "year",
                sourceName = "WindKlar aggregated national data",
                sourceUrl = "",
                sourceUpdatedAt = "",
                dataQuality = "derived",
                calculationNote = "Sum of all precomputed wind park estimates."
            )
        }
    }

    override suspend fun getWindTurbinesForPark(parkId: String): List<WindTurbine> = withContext(Dispatchers.IO) {
        windTurbineDao.getByParkId(parkId)
    }

    override suspend fun getAllWindTurbines(): List<WindTurbine> = withContext(Dispatchers.IO) {
        windTurbineDao.getAll()
    }

    override suspend fun getWindParkStatuses(): Map<String, String> = withContext(Dispatchers.IO) {
        windTurbineDao.getParkStatuses()
    }


    @OptIn(ExperimentalUuidApi::class)
    override suspend fun submitDataHint(
        category: String,
        confidence: String,
        description: String,
        status: String,
        windTurbineId: String?,
        windParkId: String?,
        municipalityId: String?,
        latitude: Double?,
        longitude: Double?,
        suggestedValue: String?,
        imageUri: String?
    ): Unit = withContext(Dispatchers.IO) {
        val id = Uuid.random().toString()
        val now = epochMillis()
        dataHintDao.insertOrReplace(
            id = id,
            category = category,
            confidence = confidence,
            status = status,
            description = description,
            windTurbineId = windTurbineId,
            windParkId = windParkId,
            municipalityId = municipalityId,
            latitude = latitude,

            longitude = longitude,
            suggestedValue = suggestedValue,
            imageUri = imageUri,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun getSnapshotAttribution(): String = withContext(Dispatchers.IO) {
        snapshotMetadataDao.getLatest()?.attribution ?: "Marktstammdatenregister"
    }

    override suspend fun getSnapshotLimitations(): List<String> = withContext(Dispatchers.IO) {
        val raw = snapshotMetadataDao.getLatest()?.limitations ?: ""
        if (raw.isBlank()) emptyList() else raw.split("\n")
    }

    private fun WindParkEntity.toDomain(isFavorite: Boolean) = WindPark(
        id = id,
        name = name,
        municipalityId = municipalityId,
        municipalityName = municipalityName,
        latitude = latitude,
        longitude = longitude,
        turbineCount = turbineCount ?: 0,
        installedCapacityKw = installedCapacityKw,
        isFavorite = isFavorite,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        sourceUpdatedAt = sourceUpdatedAt,
        dataQuality = dataQuality
    )
}

expect fun epochMillis(): Long
