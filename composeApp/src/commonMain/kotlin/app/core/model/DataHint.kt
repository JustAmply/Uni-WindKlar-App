package app.core.model

data class DataHint(
    val id: String,
    val category: String,
    val confidence: String,
    val status: String,
    val description: String,
    val windTurbineId: String?,
    val windParkId: String?,
    val municipalityId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val suggestedValue: String?,
    val imageUri: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
