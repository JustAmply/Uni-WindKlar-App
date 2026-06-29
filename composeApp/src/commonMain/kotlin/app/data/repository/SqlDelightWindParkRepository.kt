package app.data.repository

import app.core.model.WindPark
import app.core.model.Metric
import app.core.model.SnapshotAssumption
import app.core.model.SnapshotInfo
import app.core.model.WindTurbine
import app.core.model.DataHint
import app.core.model.FavoriteRegion
import app.core.model.MapSearchEntry
import app.core.model.NationalStatsSummary
import app.core.model.RegionSummary
import app.data.local.dao.*
import app.data.local.entity.SnapshotMetadata
import app.data.local.entity.WindParkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SqlDelightWindParkRepository(
    private val windParkDao: WindParkDao,
    private val windTurbineDao: WindTurbineDao,
    private val metricDao: MetricDao,
    private val favoriteDao: FavoriteDao,
    private val recentWindParkDao: RecentWindParkDao,
    private val dataHintDao: DataHintDao,
    private val snapshotMetadataDao: SnapshotMetadataDao,
    private val settingsDao: SettingsDao,
    private val summaryDao: SummaryDao,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : WindParkRepository {
    private companion object {
        const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    }

    private var cachedSnapshotInfo: SnapshotInfo? = null
    private var hasCachedSnapshotInfo = false
    private var cachedAssumptions: List<SnapshotAssumption>? = null

    private var cachedOperationalSummaryMap: Map<String, ParkOperationalSummary>? = null
    private suspend fun getOperationalSummaryMap(): Map<String, ParkOperationalSummary> {
        return cachedOperationalSummaryMap ?: summaryDao.getAllParkOperationalSummaries().also {
            cachedOperationalSummaryMap = it
        }
    }

    override suspend fun getMapStartupSnapshot(): MapStartupSnapshot = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val (parks, statuses) = summaryDao.getMapStartupParks(favorites)
        val searchEntries = summaryDao.getAllMapSearchEntries()

        MapStartupSnapshot(
            parks = parks,
            parkStatuses = statuses,
            searchEntries = searchEntries,
        )
    }

    override suspend fun getWindParks(): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val summaries = getOperationalSummaryMap()
        windParkDao.getAll()
            .filter { summaries[it.id]?.parkStatus != "Stillgelegt" }
            .map { it.toDomain(favorites.contains(it.id), summaries[it.id]) }
    }

    override suspend fun getWindPark(id: String): WindPark? = withContext(Dispatchers.Default) {
        val entity = windParkDao.getById(id) ?: return@withContext null
        val summary = getOperationalSummaryMap()[id]
        val isFav = favoriteDao.isFavorite(id)
        entity.toDomain(isFav, summary)
    }

    override suspend fun searchWindParks(query: String): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val summaries = getOperationalSummaryMap()
        windParkDao.search(query)
            .filter { summaries[it.id]?.parkStatus != "Stillgelegt" }
            .map { it.toDomain(favorites.contains(it.id), summaries[it.id]) }
    }

    override suspend fun getFavoriteWindParks(): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds()
        if (favorites.isEmpty()) return@withContext emptyList()
        val summaries = getOperationalSummaryMap()
        val entitiesMap = windParkDao.getByIds(favorites).associateBy { it.id }
        favorites.mapNotNull { id ->
            val summary = summaries[id]
            if (summary?.parkStatus == "Stillgelegt") {
                null
            } else {
                entitiesMap[id]?.toDomain(true, summary)
            }
        }
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

        favoriteEntities.mapNotNull { entity ->
            val region = summaryDao.getRegionSummary(entity.regionType.lowercase(), entity.regionId)
                ?: return@mapNotNull null
            FavoriteRegion(
                type = entity.regionType,
                id = entity.regionId,
                name = region.name
            )
        }
    }

    override suspend fun getFavoriteRegionSummaries(): List<RegionSummary> = withContext(Dispatchers.Default) {
        val favoriteEntities = favoriteDao.getFavoriteRegions()
        if (favoriteEntities.isEmpty()) return@withContext emptyList()

        favoriteEntities.mapNotNull { entity ->
            summaryDao.getRegionSummary(entity.regionType.lowercase(), entity.regionId)
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
        if (recentIds.isEmpty()) return@withContext emptyList()
        val summaries = getOperationalSummaryMap()
        val entitiesMap = windParkDao.getByIds(recentIds).associateBy { it.id }
        recentIds.mapNotNull { id ->
            val summary = summaries[id]
            if (summary?.parkStatus == "Stillgelegt") {
                null
            } else {
                entitiesMap[id]?.toDomain(favorites.contains(id), summary)
            }
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
        val summary = getNationalStatsSummary() ?: return@withContext emptyList()
        listOf(
            summary.toNationalMetric("annual_production", summary.annualProductionKwh, "kWh/a"),
            summary.toNationalMetric("co2_savings", summary.co2SavingsKg, "kg CO2/a"),
            summary.toNationalMetric("household_equivalent", summary.householdEquivalent, "households"),
            summary.toNationalMetric("municipal_participation", summary.municipalBenefitEur, "EUR/a"),
        )
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
        getNationalStatsSummary()?.activeTurbineCount ?: 0
    }

    override suspend fun getWindParkStatuses(): Map<String, String> = withContext(Dispatchers.Default) {
        getOperationalSummaryMap().mapValues { (_, summary) -> summary.parkStatus }
    }

    override suspend fun getMapSearchEntries(): List<MapSearchEntry> = withContext(Dispatchers.Default) {
        summaryDao.getAllMapSearchEntries()
    }

    override suspend fun getRegionSummaries(type: String): List<RegionSummary> = withContext(Dispatchers.Default) {
        summaryDao.getRegionSummaries(type)
    }

    override suspend fun getNationalStatsSummary(): NationalStatsSummary? = withContext(Dispatchers.Default) {
        summaryDao.getNationalStatsSummary()
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
        getSnapshotInfo()?.attribution ?: "Marktstammdatenregister"
    }

    override suspend fun getSnapshotLimitations(): List<String> = withContext(Dispatchers.Default) {
        getSnapshotInfo()?.limitations ?: emptyList()
    }

    override suspend fun getSnapshotInfo(): SnapshotInfo? = withContext(Dispatchers.Default) {
        if (hasCachedSnapshotInfo) return@withContext cachedSnapshotInfo
        val metadata = snapshotMetadataDao.getLatest()
        val limitations = metadata?.limitations
            ?.takeIf { it.isNotBlank() }
            ?.split("\n")
            ?: emptyList()
        val info = metadata?.let {
            SnapshotInfo(
                snapshotId = it.snapshotId,
                sourceName = it.sourceName,
                attribution = it.attribution,
                mastrExportDate = it.mastrExportDate,
                processedAt = it.processedAt,
                pipelineVersion = it.pipelineVersion,
                limitations = limitations,
                isLocalSnapshot = true,
            )
        }
        cachedSnapshotInfo = info
        hasCachedSnapshotInfo = true
        info
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
        cachedAssumptions?.let { return@withContext it }
        val raw = snapshotMetadataDao.getLatest()?.assumptionsJson ?: return@withContext emptyList()
        val parsed = runCatching {
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
        cachedAssumptions = parsed
        parsed
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
        summary: ParkOperationalSummary? = null
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
        turbineCount = summary?.turbineCount ?: turbineCount ?: 0,
        installedCapacityKw = summary?.capacityKw ?: installedCapacityKw,
        isFavorite = isFavorite,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        sourceUpdatedAt = sourceUpdatedAt,
        dataQuality = dataQuality
    )

    private fun NationalStatsSummary.toNationalMetric(
        metricType: String,
        value: Double,
        unit: String,
    ): Metric =
        Metric(
            id = "national_$metricType",
            subjectType = "national",
            subjectId = "DE",
            metricType = metricType,
            value = value,
            unit = unit,
            period = "year",
            sourceName = "WindKlar MVP-Berechnung",
            sourceUrl = "",
            sourceUpdatedAt = "",
            dataQuality = "estimated",
            calculationNote = "Bundesweite Summe aus vorberechneten Windpark-Metriken.",
        )

    @Serializable
    private data class SnapshotAssumptionDto(
        val id: String,
        val label: String,
        val value: Double,
        val unit: String,
        val sourceName: String,
        val sourceUrl: String,
        val sourceDate: String,
        val calculationNote: String,
    )
}

expect fun epochMillis(): Long
