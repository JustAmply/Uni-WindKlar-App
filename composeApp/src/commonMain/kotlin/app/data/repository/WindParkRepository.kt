package app.data.repository

import app.core.model.WindPark
import app.core.model.Metric
import app.core.model.WindTurbine

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
    suspend fun getMetricsForNational(): List<Metric>
    suspend fun getWindTurbinesForPark(parkId: String): List<WindTurbine>
    suspend fun getAllWindTurbines(): List<WindTurbine>
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
}

