package app.data.repository

import app.core.model.WindPark
import app.core.model.Metric
import app.core.model.SnapshotAssumption
import app.core.model.WindTurbine
import app.core.model.DataHint

interface WindParkRepository {
    suspend fun getWindParks(): List<WindPark>
    suspend fun getWindPark(id: String): WindPark?
    suspend fun searchWindParks(query: String): List<WindPark>
    
    suspend fun getFavoriteWindParks(): List<WindPark>
    suspend fun isFavorite(parkId: String): Boolean
    suspend fun setFavorite(parkId: String, isFavorite: Boolean)
    
    suspend fun getRecentWindParks(limit: Long = 10): List<WindPark>
    suspend fun recordRecentWindPark(parkId: String)
    suspend fun clearRecentWindParks()
    
    suspend fun getMetricsForPark(parkId: String): List<Metric>
    suspend fun getMetricsForNational(includeOffshore: Boolean): List<Metric>
    suspend fun isOffshoreEnabled(): Boolean
    suspend fun setOffshoreEnabled(enabled: Boolean)
    suspend fun getWindTurbinesForPark(parkId: String): List<WindTurbine>
    suspend fun getAllWindTurbines(): List<WindTurbine>
    suspend fun getWindTurbinesInBounds(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<WindTurbine>
    suspend fun countActiveWindTurbines(includeOffshore: Boolean): Int
    suspend fun getWindParkStatuses(): Map<String, String>

    
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
    suspend fun getAllMetrics(): List<Metric>
    
    suspend fun getDataHints(): List<DataHint>
}
