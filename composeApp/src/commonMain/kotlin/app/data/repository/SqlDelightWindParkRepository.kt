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
import app.data.local.source.Map_search_entry
import app.data.local.source.National_stats_summary
import app.data.local.source.Region_summary
import app.data.local.source.SourceDatabase
import app.data.local.user.UserDatabase
import app.data.local.entity.WindParkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SqlDelightWindParkRepository(
    private val sourceDatabase: SourceDatabase,
    private val userDatabase: UserDatabase,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : WindParkRepository {
    private companion object {
        const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    }

    private val windParkDao: WindParkDao = SqlDelightWindParkDao(sourceDatabase)
    private val windTurbineDao: WindTurbineDao = SqlDelightWindTurbineDao(sourceDatabase)
    private val metricDao: MetricDao = SqlDelightMetricDao(sourceDatabase)
    private val favoriteDao: FavoriteDao = SqlDelightFavoriteDao(userDatabase)
    private val recentWindParkDao: RecentWindParkDao = SqlDelightRecentWindParkDao(userDatabase)
    private val dataHintDao: DataHintDao = SqlDelightDataHintDao(userDatabase)
    private val snapshotMetadataDao: SnapshotMetadataDao = SqlDelightSnapshotMetadataDao(sourceDatabase)
    private val settingsDao: SettingsDao = SqlDelightSettingsDao(userDatabase)

    override suspend fun getMapStartupSnapshot(): MapStartupSnapshot = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val statuses = mutableMapOf<String, String>()
        val parks = sourceDatabase.summaryQueries.selectMapStartupParks(
            mapper = {
                id,
                name,
                municipality_id,
                municipality_name,
                district_id,
                district_name,
                state_id,
                state_name,
                latitude,
                longitude,
                grouping_method,
                source_name,
                source_url,
                source_updated_at,
                data_quality,
                park_status,
                valid_turbine_count,
                valid_capacity_kw,
                ->
                statuses[id] = park_status
                WindPark(
                    id = id,
                    name = name,
                    municipalityId = municipality_id,
                    municipalityName = municipality_name,
                    districtId = district_id,
                    districtName = district_name,
                    stateId = state_id,
                    stateName = state_name,
                    latitude = latitude,
                    longitude = longitude,
                    turbineCount = valid_turbine_count.toInt(),
                    installedCapacityKw = valid_capacity_kw,
                    isFavorite = favorites.contains(id),
                    sourceName = source_name,
                    sourceUrl = source_url,
                    sourceUpdatedAt = source_updated_at,
                    dataQuality = data_quality,
                )
            },
        ).executeAsList()
        val searchEntries = sourceDatabase.summaryQueries
            .selectAllMapSearchEntries()
            .executeAsList()
            .map { it.toDomain() }

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
            .map { it.toDomain(favorites.contains(it.id), summaries[it.id]?.validStats) }
    }

    override suspend fun getWindPark(id: String): WindPark? = withContext(Dispatchers.Default) {
        val entity = windParkDao.getById(id) ?: return@withContext null
        val summary = getOperationalSummaryMap()[id]
        if (summary?.parkStatus == "Stillgelegt") return@withContext null
        val isFav = favoriteDao.isFavorite(id)
        entity.toDomain(isFav, summary?.validStats)
    }

    override suspend fun searchWindParks(query: String): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val summaries = getOperationalSummaryMap()
        windParkDao.search(query)
            .filter { summaries[it.id]?.parkStatus != "Stillgelegt" }
            .map { it.toDomain(favorites.contains(it.id), summaries[it.id]?.validStats) }
    }

    override suspend fun getFavoriteWindParks(): List<WindPark> = withContext(Dispatchers.Default) {
        val favorites = favoriteDao.getFavoriteIds().toSet()
        val summaries = getOperationalSummaryMap()
        windParkDao.getAll()
            .filter { favorites.contains(it.id) && summaries[it.id]?.parkStatus != "Stillgelegt" }
            .map { it.toDomain(true, summaries[it.id]?.validStats) }
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
            val region = sourceDatabase.summaryQueries
                .selectRegionSummary(entity.regionType.lowercase(), entity.regionId)
                .executeAsOneOrNull()
                ?: return@mapNotNull null
            FavoriteRegion(
                type = entity.regionType,
                id = entity.regionId,
                name = region.name
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
        val summaries = getOperationalSummaryMap()
        recentIds.mapNotNull { id ->
            val summary = summaries[id]
            if (summary?.parkStatus == "Stillgelegt") {
                null
            } else {
                windParkDao.getById(id)?.toDomain(favorites.contains(id), summary?.validStats)
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
        sourceDatabase.summaryQueries.selectAllMapSearchEntries().executeAsList().map { it.toDomain() }
    }

    override suspend fun getRegionSummaries(type: String): List<RegionSummary> = withContext(Dispatchers.Default) {
        sourceDatabase.summaryQueries.selectRegionSummaries(type.lowercase()).executeAsList().map { it.toDomain() }
    }

    override suspend fun getNationalStatsSummary(): NationalStatsSummary? = withContext(Dispatchers.Default) {
        sourceDatabase.summaryQueries.selectNationalStatsSummary().executeAsOneOrNull()?.toDomain()
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

    private fun getOperationalSummaryMap(): Map<String, OperationalSummary> =
        sourceDatabase.summaryQueries.selectAllParkOperationalSummaries().executeAsList().associate { row ->
            row.wind_park_id to OperationalSummary(
                parkStatus = row.park_status,
                validStats = ValidParkStats(
                    turbineCount = row.valid_turbine_count.toInt(),
                    capacityKw = row.valid_capacity_kw,
                ),
            )
        }

    private fun Map_search_entry.toDomain(): MapSearchEntry =
        MapSearchEntry(
            id = id,
            resultType = result_type,
            targetId = target_id,
            label = label,
            description = description,
            latitude = latitude,
            longitude = longitude,
            typeRank = type_rank.toInt(),
            haystack = haystack,
            sortName = sort_name,
        )

    private fun Region_summary.toDomain(): RegionSummary =
        RegionSummary(
            regionType = region_type,
            regionId = region_id,
            name = name,
            contextLabel = context_label,
            parentName = parent_name,
            latitude = latitude,
            longitude = longitude,
            windParkCount = wind_park_count.toInt(),
            turbineCount = turbine_count.toInt(),
            installedCapacityKw = installed_capacity_kw,
            annualProductionKwh = annual_production_kwh,
            co2SavingsKg = co2_savings_kg,
            householdEquivalent = household_equivalent,
            municipalBenefitEur = municipal_benefit_eur,
        )

    private fun National_stats_summary.toDomain(): NationalStatsSummary =
        NationalStatsSummary(
            windParkCount = wind_park_count.toInt(),
            activeTurbineCount = active_turbine_count.toInt(),
            installedCapacityKw = installed_capacity_kw,
            annualProductionKwh = annual_production_kwh,
            co2SavingsKg = co2_savings_kg,
            householdEquivalent = household_equivalent,
            municipalBenefitEur = municipal_benefit_eur,
            capacityClassLt5Mw = capacity_class_lt_5mw.toInt(),
            capacityClass5To20Mw = capacity_class_5_20mw.toInt(),
            capacityClass20To50Mw = capacity_class_20_50mw.toInt(),
            capacityClassGte50Mw = capacity_class_gte_50mw.toInt(),
            turbineCommissioningPre2000 = turbine_commissioning_pre_2000.toInt(),
            turbineCommissioning2000To2009 = turbine_commissioning_2000_2009.toInt(),
            turbineCommissioning2010To2019 = turbine_commissioning_2010_2019.toInt(),
            turbineCommissioning2020Plus = turbine_commissioning_2020_plus.toInt(),
            turbineCommissioningUnknown = turbine_commissioning_unknown.toInt(),
            turbineHeightLt80m = turbine_height_lt_80m.toInt(),
            turbineHeight80To120m = turbine_height_80_120m.toInt(),
            turbineHeight120To160m = turbine_height_120_160m.toInt(),
            turbineHeightGte160m = turbine_height_gte_160m.toInt(),
            turbineHeightUnknown = turbine_height_unknown.toInt(),
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

    private data class OperationalSummary(
        val parkStatus: String,
        val validStats: ValidParkStats,
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
