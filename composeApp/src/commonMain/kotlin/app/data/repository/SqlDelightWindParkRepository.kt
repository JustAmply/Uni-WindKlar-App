package app.data.repository

import app.core.model.WindPark
import app.core.model.Metric
import app.core.model.SnapshotAssumption
import app.core.model.SnapshotInfo
import app.core.model.WindTurbine
import app.core.model.DataHint
import app.core.model.FavoriteRegion
import app.data.local.dao.*
import app.data.local.entity.WindParkEntity
import app.data.local.db.AppDatabase
import app.data.snapshot.SnapshotAssumptionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SqlDelightWindParkRepository(
    private val database: AppDatabase,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : WindParkRepository {
    private companion object {
        const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    }

    private val windParkDao: WindParkDao = SqlDelightWindParkDao(database)
    private val windTurbineDao: WindTurbineDao = SqlDelightWindTurbineDao(database)
    private val metricDao: MetricDao = SqlDelightMetricDao(database)
    private val favoriteDao: FavoriteDao = SqlDelightFavoriteDao(database)
    private val recentWindParkDao: RecentWindParkDao = SqlDelightRecentWindParkDao(database)
    private val dataHintDao: DataHintDao = SqlDelightDataHintDao(database)
    private val snapshotMetadataDao: SnapshotMetadataDao = SqlDelightSnapshotMetadataDao(database)
    private val settingsDao: SettingsDao = SqlDelightSettingsDao(database)

    override suspend fun getWindParks(): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val statuses = windTurbineDao.getParkStatuses()
        val validStats = windTurbineDao.getValidParkStats()
        windParkDao.getAll()
            .filter { statuses[it.id] != "Stillgelegt" }
            .map { it.toDomain(favorites.contains(it.id), validStats[it.id]) }
    }

    override suspend fun getWindPark(id: String): WindPark? = withContext(Dispatchers.Default) {
        val entity = windParkDao.getById(id) ?: return@withContext null
        val statuses = windTurbineDao.getParkStatuses()
        if (statuses[id] == "Stillgelegt") return@withContext null
        val isFav = favoriteDao.isFavorite(id)
        val validStats = windTurbineDao.getValidParkStats()[id]
        entity.toDomain(isFav, validStats)
    }

    override suspend fun searchWindParks(query: String): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val statuses = windTurbineDao.getParkStatuses()
        val validStats = windTurbineDao.getValidParkStats()
        windParkDao.search(query)
            .filter { statuses[it.id] != "Stillgelegt" }
            .map { it.toDomain(favorites.contains(it.id), validStats[it.id]) }
    }

    override suspend fun getFavoriteWindParks(): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val statuses = windTurbineDao.getParkStatuses()
        val validStats = windTurbineDao.getValidParkStats()
        windParkDao.getAll()
            .filter { favorites.contains(it.id) && statuses[it.id] != "Stillgelegt" }
            .map { it.toDomain(true, validStats[it.id]) }
    }

    override suspend fun isFavorite(parkId: String): Boolean = withContext(Dispatchers.Default) {
        favoriteDao.isFavorite(parkId)
    }

    override suspend fun setFavorite(parkId: String, isFavorite: Boolean): Unit = withContext(Dispatchers.Default) {
        if (isFavorite) {
            favoriteDao.addFavorite(parkId, epochMillis())
        } else {
            favoriteDao.removeFavorite(parkId)
        }
    }

    override suspend fun getFavoriteRegions(): List<FavoriteRegion> = withContext(Dispatchers.Default) {
        val favoriteEntities = favoriteDao.getFavoriteRegions()
        if (favoriteEntities.isEmpty()) return@withContext emptyList()
        
        val statuses = windTurbineDao.getParkStatuses()
        val allParks = windParkDao.getAll().filter { statuses[it.id] != "Stillgelegt" }
        
        favoriteEntities.mapNotNull { entity ->
            val regionParks = allParks.filter { park ->
                when (entity.regionType.lowercase()) {
                    "city" -> park.municipalityId == entity.regionId
                    "district" -> park.districtId == entity.regionId
                    "state" -> park.stateId == entity.regionId
                    else -> false
                }
            }
            val firstPark = regionParks.firstOrNull() ?: return@mapNotNull null
            val regionName = when (entity.regionType.lowercase()) {
                "city" -> firstPark.municipalityName
                "district" -> firstPark.districtName
                "state" -> firstPark.stateName
                else -> ""
            }
            FavoriteRegion(
                type = entity.regionType,
                id = entity.regionId,
                name = regionName
            )
        }
    }

    override suspend fun isRegionFavorite(type: String, id: String): Boolean = withContext(Dispatchers.Default) {
        favoriteDao.isRegionFavorite(type, id)
    }

    override suspend fun setRegionFavorite(type: String, id: String, isFavorite: Boolean): Unit = withContext(Dispatchers.Default) {
        if (isFavorite) {
            favoriteDao.addRegionFavorite(type, id, epochMillis())
        } else {
            favoriteDao.removeRegionFavorite(type, id)
        }
    }

    override suspend fun getRecentWindParks(limit: Long): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val recentIds = recentWindParkDao.getRecentWindParkIds(limit)
        val statuses = windTurbineDao.getParkStatuses()
        val validStats = windTurbineDao.getValidParkStats()
        val entitiesMap = windParkDao.getAll()
            .filter { recentIds.contains(it.id) && statuses[it.id] != "Stillgelegt" }
            .associateBy { it.id }
        recentIds.mapNotNull { id ->
            entitiesMap[id]?.toDomain(favorites.contains(id), validStats[id])
        }
    }

    override suspend fun recordRecentWindPark(parkId: String): Unit = withContext(Dispatchers.Default) {
        recentWindParkDao.recordRecentWindPark(parkId, epochMillis())
    }

    override suspend fun clearRecentWindParks(): Unit = withContext(Dispatchers.Default) {
        recentWindParkDao.clear()
    }

    override suspend fun getMetricsForPark(parkId: String): List<Metric> = withContext(Dispatchers.Default) {
        metricDao.getForSubject("wind_park", parkId)
    }

    override suspend fun getMetricsForNational(): List<Metric> = withContext(Dispatchers.Default) {
        metricDao.getNationalAggregates()
    }

    override suspend fun getWindTurbinesForPark(parkId: String): List<WindTurbine> = withContext(Dispatchers.Default) {
        windTurbineDao.getByParkId(parkId)
    }

    override suspend fun getAllWindTurbines(): List<WindTurbine> = withContext(Dispatchers.Default) {
        windTurbineDao.getAll()
    }

    override suspend fun getWindTurbinesInBounds(
        swLat: Double,
        swLon: Double,
        neLat: Double,
        neLon: Double,
    ): List<WindTurbine> = withContext(Dispatchers.Default) {
        windTurbineDao.getInBounds(swLat, swLon, neLat, neLon)
    }

    override suspend fun countActiveWindTurbines(): Int = withContext(Dispatchers.Default) {
        windTurbineDao.countActive()
    }

    override suspend fun getWindParkStatuses(): Map<String, String> = withContext(Dispatchers.Default) {
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
    ): Unit = withContext(Dispatchers.Default) {
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

    override suspend fun getSnapshotAttribution(): String = withContext(Dispatchers.Default) {
        snapshotMetadataDao.getLatest()?.attribution ?: "Marktstammdatenregister"
    }

    override suspend fun getSnapshotLimitations(): List<String> = withContext(Dispatchers.Default) {
        val raw = snapshotMetadataDao.getLatest()?.limitations ?: ""
        if (raw.isBlank()) emptyList() else raw.split("\n")
    }

    override suspend fun getSnapshotInfo(): SnapshotInfo? = withContext(Dispatchers.Default) {
        val metadata = snapshotMetadataDao.getLatest() ?: return@withContext null
        val limitations = metadata.limitations
            .takeIf { it.isNotBlank() }
            ?.split("\n")
            ?: emptyList()
        SnapshotInfo(
            snapshotId = metadata.snapshot_id,
            sourceName = metadata.source_name,
            attribution = metadata.attribution,
            mastrExportDate = metadata.mastr_export_date,
            processedAt = metadata.processed_at,
            pipelineVersion = metadata.pipeline_version,
            limitations = limitations,
            isLocalSnapshot = true,
        )
    }

    override suspend fun getDataHints(): List<DataHint> = withContext(Dispatchers.Default) {
        dataHintDao.getAll()
    }

    override suspend fun isOnboardingCompleted(): Boolean = withContext(Dispatchers.Default) {
        settingsDao.getValue(ONBOARDING_COMPLETED_KEY)
            ?.trim()
            ?.lowercase()
            ?.let { it == "true" }
            ?: false
    }

    override suspend fun setOnboardingCompleted(completed: Boolean): Unit = withContext(Dispatchers.Default) {
        settingsDao.upsertValue(ONBOARDING_COMPLETED_KEY, completed.toString())
    }

    override suspend fun getSnapshotAssumptions(): List<SnapshotAssumption> = withContext(Dispatchers.Default) {
        val raw = snapshotMetadataDao.getLatest()?.assumptions_json ?: return@withContext emptyList()
        runCatching {
            json.decodeFromString<List<SnapshotAssumptionDto>>(raw).map { assumption ->
                SnapshotAssumption(
                    id = assumption.id,
                    label = assumption.label,
                    value = assumption.value,
                    unit = assumption.unit,
                    sourceName = assumption.sourceName,
                    sourceUrl = assumption.sourceUrl,
                    sourceDate = assumption.sourceDate,
                    calculationNote = assumption.calculationNote,
                )
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun getAllMetrics(): List<Metric> = withContext(Dispatchers.Default) {
        metricDao.getAll()
    }

    override suspend fun getMetricsForParks(parkIds: List<String>): List<Metric> = withContext(Dispatchers.Default) {
        if (parkIds.isEmpty()) return@withContext emptyList()
        metricDao.getForSubjects("wind_park", parkIds)
    }

    private fun WindParkEntity.toDomain(
        isFavorite: Boolean,
        validStats: ValidParkStats? = null
    ) = WindPark(
        id = id,
        name = name,
        municipalityId = municipalityId,
        municipalityName = municipalityName,
        districtId = districtId,
        districtName = districtName,
        stateId = stateId,
        stateName = stateName,
        latitude = latitude,
        longitude = longitude,
        turbineCount = validStats?.turbineCount ?: turbineCount ?: 0,
        installedCapacityKw = validStats?.capacityKw ?: installedCapacityKw,
        isFavorite = isFavorite,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        sourceUpdatedAt = sourceUpdatedAt,
        dataQuality = dataQuality
    )
}

expect fun epochMillis(): Long
