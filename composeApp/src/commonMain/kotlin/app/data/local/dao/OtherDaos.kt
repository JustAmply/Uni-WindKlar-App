package app.data.local.dao

import app.data.local.db.AppDatabase
import app.data.local.db.Wind_turbine
import app.data.local.db.Metric as DbMetric
import app.data.local.db.Snapshot_metadata
import app.data.local.db.Data_hint
import app.core.model.WindTurbine
import app.core.model.Metric
import app.core.model.DataHint

// --- Turbine DAO ---
interface WindTurbineDao {
    suspend fun getByParkId(parkId: String): List<WindTurbine>
    suspend fun getAll(): List<WindTurbine>
    suspend fun getInBounds(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<WindTurbine>
    suspend fun countActive(): Int
    suspend fun getParkStatuses(): Map<String, String>
    suspend fun getValidParkStats(): Map<String, ValidParkStats>
    suspend fun insertOrReplace(turbine: WindTurbine)
}

data class ValidParkStats(
    val turbineCount: Int,
    val capacityKw: Long,
)

class SqlDelightWindTurbineDao(private val database: AppDatabase) : WindTurbineDao {
    override suspend fun getByParkId(parkId: String): List<WindTurbine> {
        return database.windTurbineQueries.selectWindTurbinesByParkId(parkId).executeAsList().map { it.toDomain() }
    }

    override suspend fun getAll(): List<WindTurbine> {
        return database.windTurbineQueries.selectAllWindTurbines().executeAsList().map { it.toDomain() }
    }

    override suspend fun getInBounds(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<WindTurbine> {
        return database.windTurbineQueries
            .selectWindTurbinesInBounds(
                swLat = swLat,
                swLon = swLon,
                neLat = neLat,
                neLon = neLon,
            )
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun countActive(): Int {
        return database.windTurbineQueries.countActiveWindTurbines().executeAsOne().toInt()
    }

    override suspend fun getParkStatuses(): Map<String, String> {
        return database.windTurbineQueries.selectParkStatuses().executeAsList().associate { row ->
            row.wind_park_id to row.park_status
        }
    }

    override suspend fun getValidParkStats(): Map<String, ValidParkStats> {
        return database.windTurbineQueries.selectValidParkStats().executeAsList().associate { row ->
            row.wind_park_id to ValidParkStats(
                turbineCount = row.valid_turbine_count.toInt(),
                capacityKw = row.valid_capacity_kw?.toLong() ?: 0L
            )
        }
    }
    override suspend fun insertOrReplace(turbine: WindTurbine) {
        database.windTurbineQueries.upsertWindTurbine(
            id = turbine.id,
            wind_park_id = turbine.windParkId,
            name = turbine.name,
            municipality_id = turbine.municipalityId,
            municipality_name = turbine.municipalityName,
            district_id = turbine.districtId,
            district_name = turbine.districtName,
            state_id = turbine.stateId,
            state_name = turbine.stateName,
            latitude = turbine.latitude,
            longitude = turbine.longitude,
            installed_capacity_kw = turbine.installedCapacityKw,
            status = turbine.status,
            turbine_type = turbine.turbineType,
            manufacturer = turbine.manufacturer,
            model = turbine.model,
            hub_height_m = turbine.hubHeightM,
            rotor_diameter_m = turbine.rotorDiameterM,
            commissioning_year = turbine.commissioningYear,
            source_name = turbine.sourceName,
            source_url = turbine.sourceUrl,
            source_updated_at = turbine.sourceUpdatedAt,
            data_quality = turbine.dataQuality
        )
        database.windTurbineQueries.updateWindTurbine(
            id = turbine.id,
            wind_park_id = turbine.windParkId,
            name = turbine.name,
            municipality_id = turbine.municipalityId,
            municipality_name = turbine.municipalityName,
            district_id = turbine.districtId,
            district_name = turbine.districtName,
            state_id = turbine.stateId,
            state_name = turbine.stateName,
            latitude = turbine.latitude,
            longitude = turbine.longitude,
            installed_capacity_kw = turbine.installedCapacityKw,
            status = turbine.status,
            turbine_type = turbine.turbineType,
            manufacturer = turbine.manufacturer,
            model = turbine.model,
            hub_height_m = turbine.hubHeightM,
            rotor_diameter_m = turbine.rotorDiameterM,
            commissioning_year = turbine.commissioningYear,
            source_name = turbine.sourceName,
            source_url = turbine.sourceUrl,
            source_updated_at = turbine.sourceUpdatedAt,
            data_quality = turbine.dataQuality
        )
    }

    private fun Wind_turbine.toDomain() = WindTurbine(
        id = id,
        windParkId = wind_park_id,
        name = name,
        municipalityId = municipality_id,
        municipalityName = municipality_name,
        districtId = district_id,
        districtName = district_name,
        stateId = state_id,
        stateName = state_name,
        latitude = latitude,
        longitude = longitude,
        installedCapacityKw = installed_capacity_kw,
        status = status,
        turbineType = turbine_type,
        manufacturer = manufacturer,
        model = model,
        hubHeightM = hub_height_m,
        rotorDiameterM = rotor_diameter_m,
        commissioningYear = commissioning_year,
        sourceName = source_name,
        sourceUrl = source_url,
        sourceUpdatedAt = source_updated_at,
        dataQuality = data_quality
    )
}

// --- Metric DAO ---

interface MetricDao {
    suspend fun getForSubject(subjectType: String, subjectId: String): List<Metric>
    suspend fun getForSubjects(subjectType: String, subjectIds: List<String>): List<Metric>
    suspend fun getAll(): List<Metric>
    suspend fun getNationalAggregates(): List<Metric>
    suspend fun insertOrReplace(metric: Metric)
}

class SqlDelightMetricDao(private val database: AppDatabase) : MetricDao {
    override suspend fun getForSubject(subjectType: String, subjectId: String): List<Metric> {
        return database.metricQueries.selectMetricsForSubject(subjectType, subjectId).executeAsList().map { it.toDomain() }
    }

    override suspend fun getForSubjects(subjectType: String, subjectIds: List<String>): List<Metric> {
        if (subjectIds.isEmpty()) return emptyList()
        return database.metricQueries.selectMetricsForSubjects(subjectType, subjectIds).executeAsList().map { it.toDomain() }
    }

    override suspend fun getAll(): List<Metric> {
        return database.metricQueries.selectAllMetrics().executeAsList().map { it.toDomain() }
    }

    override suspend fun getNationalAggregates(): List<Metric> {
        return database.metricQueries.selectNationalMetricAggregates().executeAsList().map { row ->
            Metric(
                id = "national_${row.metric_type}",
                subjectType = "national",
                subjectId = "DE",
                metricType = row.metric_type,
                value = if (row.present_value_count > 0L) row.metric_value else null,
                unit = row.unit.orEmpty(),
                period = row.period,
                sourceName = row.source_name ?: "WindKlar MVP-Berechnung",
                sourceUrl = row.source_url.orEmpty(),
                sourceUpdatedAt = row.source_updated_at.orEmpty(),
                dataQuality = row.data_quality,
                calculationNote = row.calculation_note?.let {
                    "Bundesweite Summe aus Windpark-Metriken. $it"
                } ?: "Bundesweite Summe aus Windpark-Metriken.",
            )
        }
    }

    override suspend fun insertOrReplace(metric: Metric) {
        database.metricQueries.upsertMetric(
            id = metric.id,
            subject_type = metric.subjectType,
            subject_id = metric.subjectId,
            metric_type = metric.metricType,
            metric_value = metric.value,
            unit = metric.unit,
            period = metric.period,
            source_name = metric.sourceName,
            source_url = metric.sourceUrl,
            source_updated_at = metric.sourceUpdatedAt,
            data_quality = metric.dataQuality,
            calculation_note = metric.calculationNote
        )
        database.metricQueries.updateMetric(
            id = metric.id,
            subject_type = metric.subjectType,
            subject_id = metric.subjectId,
            metric_type = metric.metricType,
            metric_value = metric.value,
            unit = metric.unit,
            period = metric.period,
            source_name = metric.sourceName,
            source_url = metric.sourceUrl,
            source_updated_at = metric.sourceUpdatedAt,
            data_quality = metric.dataQuality,
            calculation_note = metric.calculationNote
        )
    }

    private fun DbMetric.toDomain() = Metric(
        id = id,
        subjectType = subject_type,
        subjectId = subject_id,
        metricType = metric_type,
        value = metric_value,
        unit = unit,
        period = period,
        sourceName = source_name,
        sourceUrl = source_url,
        sourceUpdatedAt = source_updated_at,
        dataQuality = data_quality,
        calculationNote = calculation_note
    )
}

// --- Favorite DAO ---

data class FavoriteRegionEntity(
    val regionType: String,
    val regionId: String,
    val createdAtEpochMillis: Long,
)

interface FavoriteDao {
    suspend fun getFavoriteIds(): List<String>
    suspend fun isFavorite(parkId: String): Boolean
    suspend fun addFavorite(parkId: String, timestamp: Long)
    suspend fun removeFavorite(parkId: String)

    suspend fun getFavoriteRegions(): List<FavoriteRegionEntity>
    suspend fun isRegionFavorite(type: String, id: String): Boolean
    suspend fun addRegionFavorite(type: String, id: String, timestamp: Long)
    suspend fun removeRegionFavorite(type: String, id: String)
}

class SqlDelightFavoriteDao(private val database: AppDatabase) : FavoriteDao {
    override suspend fun getFavoriteIds(): List<String> {
        return database.favoriteQueries.selectFavoriteIds().executeAsList()
    }

    override suspend fun isFavorite(parkId: String): Boolean {
        return database.favoriteQueries.isFavorite(parkId).executeAsOne()
    }

    override suspend fun addFavorite(parkId: String, timestamp: Long) {
        database.favoriteQueries.addFavorite(parkId, timestamp)
        database.favoriteQueries.updateFavoriteTimestamp(timestamp, parkId)
    }

    override suspend fun removeFavorite(parkId: String) {
        database.favoriteQueries.removeFavorite(parkId)
    }

    override suspend fun getFavoriteRegions(): List<FavoriteRegionEntity> {
        return database.favoriteQueries.selectFavoriteRegions().executeAsList().map {
            FavoriteRegionEntity(
                regionType = it.region_type,
                regionId = it.region_id,
                createdAtEpochMillis = it.created_at_epoch_millis
            )
        }
    }

    override suspend fun isRegionFavorite(type: String, id: String): Boolean {
        return database.favoriteQueries.isRegionFavorite(type, id).executeAsOne()
    }

    override suspend fun addRegionFavorite(type: String, id: String, timestamp: Long) {
        database.favoriteQueries.addRegionFavorite(type, id, timestamp)
        database.favoriteQueries.updateRegionFavoriteTimestamp(timestamp, type, id)
    }

    override suspend fun removeRegionFavorite(type: String, id: String) {
        database.favoriteQueries.removeRegionFavorite(type, id)
    }
}

// --- Recent Wind Park DAO ---

interface RecentWindParkDao {
    suspend fun getRecentWindParkIds(limit: Long): List<String>
    suspend fun recordRecentWindPark(parkId: String, timestamp: Long)
    suspend fun clear()
}

class SqlDelightRecentWindParkDao(private val database: AppDatabase) : RecentWindParkDao {
    override suspend fun getRecentWindParkIds(limit: Long): List<String> {
        return database.recentWindParkQueries.selectRecentWindParks(limit).executeAsList()
    }

    override suspend fun recordRecentWindPark(parkId: String, timestamp: Long) {
        database.recentWindParkQueries.recordRecentWindPark(parkId, timestamp)
        database.recentWindParkQueries.updateRecentWindParkTimestamp(timestamp, parkId)
    }

    override suspend fun clear() {
        database.recentWindParkQueries.clearRecentWindParks()
    }
}

// --- Data Hint DAO ---

interface DataHintDao {
    suspend fun getAll(): List<DataHint>
    suspend fun insertOrReplace(
        id: String,
        category: String,
        confidence: String,
        status: String,
        description: String,
        windTurbineId: String?,
        windParkId: String?,
        municipalityId: String?,
        latitude: Double?,
        longitude: Double?,
        suggestedValue: String?,
        imageUri: String?,
        createdAt: Long,
        updatedAt: Long
    )
}

class SqlDelightDataHintDao(private val database: AppDatabase) : DataHintDao {
    override suspend fun getAll(): List<DataHint> {
        return database.dataHintQueries.selectDataHints().executeAsList().map { it.toDomain() }
    }

    override suspend fun insertOrReplace(
        id: String,
        category: String,
        confidence: String,
        status: String,
        description: String,
        windTurbineId: String?,
        windParkId: String?,
        municipalityId: String?,
        latitude: Double?,
        longitude: Double?,
        suggestedValue: String?,
        imageUri: String?,
        createdAt: Long,
        updatedAt: Long
    ) {
        database.dataHintQueries.upsertDataHint(
            id = id,
            category = category,
            confidence = confidence,
            status = status,
            description = description,
            wind_turbine_id = windTurbineId,
            wind_park_id = windParkId,
            municipality_id = municipalityId,
            latitude = latitude,
            longitude = longitude,
            suggested_value = suggestedValue,
            image_uri = imageUri,
            created_at_epoch_millis = createdAt,
            updated_at_epoch_millis = updatedAt
        )
        database.dataHintQueries.updateDataHint(
            id = id,
            category = category,
            confidence = confidence,
            status = status,
            description = description,
            wind_turbine_id = windTurbineId,
            wind_park_id = windParkId,
            municipality_id = municipalityId,
            latitude = latitude,
            longitude = longitude,
            suggested_value = suggestedValue,
            image_uri = imageUri,
            created_at_epoch_millis = createdAt,
            updated_at_epoch_millis = updatedAt
        )
    }

    private fun Data_hint.toDomain() = DataHint(
        id = id,
        category = category,
        confidence = confidence,
        status = status,
        description = description,
        windTurbineId = wind_turbine_id,
        windParkId = wind_park_id,
        municipalityId = municipality_id,
        latitude = latitude,
        longitude = longitude,
        suggestedValue = suggested_value,
        imageUri = image_uri,
        createdAtEpochMillis = created_at_epoch_millis,
        updatedAtEpochMillis = updated_at_epoch_millis
    )
}

// --- Snapshot Metadata DAO ---

interface SnapshotMetadataDao {
    suspend fun getLatest(): Snapshot_metadata?
}

class SqlDelightSnapshotMetadataDao(private val database: AppDatabase) : SnapshotMetadataDao {
    override suspend fun getLatest(): Snapshot_metadata? {
        return database.snapshotMetadataQueries.selectLatestSnapshot().executeAsOneOrNull()
    }
}

// --- Settings DAO ---

interface SettingsDao {
    suspend fun getValue(key: String): String?
    suspend fun upsertValue(key: String, value: String)
}

class SqlDelightSettingsDao(private val database: AppDatabase) : SettingsDao {
    override suspend fun getValue(key: String): String? {
        return database.settingQueries.getSetting(key).executeAsOneOrNull()
    }

    override suspend fun upsertValue(key: String, value: String) {
        database.settingQueries.upsertSetting(key, value)
        database.settingQueries.updateSetting(value, key)
    }
}


