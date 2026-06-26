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

interface WindParkRepository {
    suspend fun getMapStartupSnapshot(): MapStartupSnapshot
    suspend fun getWindParks(): List<WindPark>
    suspend fun getWindPark(id: String): WindPark?
    suspend fun searchWindParks(query: String): List<WindPark>
    
    suspend fun getFavoriteWindParks(): List<WindPark>
    suspend fun isFavorite(parkId: String): Boolean
    suspend fun setFavorite(parkId: String, isFavorite: Boolean)
    
    suspend fun getFavoriteRegions(): List<FavoriteRegion>
    suspend fun isRegionFavorite(type: String, id: String): Boolean
    suspend fun setRegionFavorite(type: String, id: String, isFavorite: Boolean)
    
    suspend fun getRecentWindParks(limit: Long = 10): List<WindPark>
    suspend fun recordRecentWindPark(parkId: String)
    suspend fun clearRecentWindParks()
    
    suspend fun getMetricsForPark(parkId: String): List<Metric>
    suspend fun getMetricsForNational(): List<Metric>
    suspend fun getWindTurbinesForPark(parkId: String): List<WindTurbine>
    suspend fun getAllWindTurbines(): List<WindTurbine>
    suspend fun getWindTurbinesInBounds(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<WindTurbine>
    suspend fun countActiveWindTurbines(): Int
    suspend fun getWindParkStatuses(): Map<String, String>
    suspend fun getMapSearchEntries(): List<MapSearchEntry>
    suspend fun getRegionSummaries(type: String): List<RegionSummary>
    suspend fun getNationalStatsSummary(): NationalStatsSummary?

    
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
    
    suspend fun getSnapshotAttribution(): String
    suspend fun getSnapshotLimitations(): List<String>
    suspend fun getSnapshotAssumptions(): List<SnapshotAssumption>
    suspend fun getSnapshotInfo(): SnapshotInfo?
    suspend fun getAllMetrics(): List<Metric>
    suspend fun getMetricsForParks(parkIds: List<String>): List<Metric>
    
    suspend fun getDataHints(): List<DataHint>
    
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)
}
