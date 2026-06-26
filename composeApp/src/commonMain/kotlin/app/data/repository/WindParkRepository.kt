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

data class MapStartupSnapshot(
    val parks: List<WindPark>,
    val parkStatuses: Map<String, String>,
    val searchEntries: List<MapSearchEntry>,
)

interface AppSettingsRepository {
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)
}

interface MapRepository {
    suspend fun getMapStartupSnapshot(): MapStartupSnapshot
    suspend fun getRecentWindParks(limit: Long): List<WindPark>
    suspend fun recordRecentWindPark(parkId: String)
    suspend fun getMetricsForPark(parkId: String): List<Metric>
    suspend fun getWindTurbinesInBounds(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<WindTurbine>
    suspend fun getRegionSummaries(type: String): List<RegionSummary>
}

interface StatsRepository {
    suspend fun getWindParks(): List<WindPark>
    suspend fun getRecentWindParks(limit: Long): List<WindPark>
    suspend fun getMetricsForPark(parkId: String): List<Metric>
    suspend fun getRegionSummaries(type: String): List<RegionSummary>
    suspend fun getNationalStatsSummary(): NationalStatsSummary?
    suspend fun getSnapshotAttribution(): String
    suspend fun getSnapshotAssumptions(): List<SnapshotAssumption>
    suspend fun getSnapshotInfo(): SnapshotInfo?
}

interface SavedPlacesRepository {
    suspend fun getFavoriteWindParks(): List<WindPark>
    suspend fun getFavoriteRegions(): List<FavoriteRegion>
    suspend fun getRecentWindParks(limit: Long): List<WindPark>
    suspend fun getWindParks(): List<WindPark>
    suspend fun getMetricsForParks(parkIds: List<String>): List<Metric>
}

interface ProfileRepository {
    suspend fun getSnapshotAttribution(): String
    suspend fun getSnapshotLimitations(): List<String>
    suspend fun clearRecentWindParks()
    suspend fun getDataHints(): List<DataHint>
}

interface ParkDetailRepository {
    suspend fun getWindPark(id: String): WindPark?
    suspend fun getWindTurbinesForPark(parkId: String): List<WindTurbine>
    suspend fun getMetricsForPark(parkId: String): List<Metric>
    suspend fun getSnapshotAssumptions(): List<SnapshotAssumption>
    suspend fun getSnapshotAttribution(): String
    suspend fun recordRecentWindPark(parkId: String)
    suspend fun isFavorite(parkId: String): Boolean
    suspend fun setFavorite(parkId: String, isFavorite: Boolean)
}

interface RegionDetailRepository {
    suspend fun getWindParks(): List<WindPark>
    suspend fun getMetricsForParks(parkIds: List<String>): List<Metric>
    suspend fun getSnapshotAssumptions(): List<SnapshotAssumption>
    suspend fun getSnapshotAttribution(): String
    suspend fun isRegionFavorite(type: String, id: String): Boolean
    suspend fun setRegionFavorite(type: String, id: String, isFavorite: Boolean)
}

interface DataHintRepository {
    suspend fun submitDataHint(
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
        imageUri: String?,
    )
}

interface WindParkRepository :
    AppSettingsRepository,
    MapRepository,
    StatsRepository,
    SavedPlacesRepository,
    ProfileRepository,
    ParkDetailRepository,
    RegionDetailRepository,
    DataHintRepository {
    suspend fun searchWindParks(query: String): List<WindPark>

    suspend fun getMetricsForNational(): List<Metric>
    suspend fun getAllWindTurbines(): List<WindTurbine>
    suspend fun countActiveWindTurbines(): Int
    suspend fun getWindParkStatuses(): Map<String, String>
    suspend fun getMapSearchEntries(): List<MapSearchEntry>
    suspend fun getAllMetrics(): List<Metric>
}
