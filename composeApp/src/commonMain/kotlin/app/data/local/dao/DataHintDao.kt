package app.data.local.dao

import app.data.local.user.UserDatabase
import app.data.local.user.Data_hint
import app.core.model.DataHint

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

class SqlDelightDataHintDao(private val database: UserDatabase) : DataHintDao {
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
