package app.data.local.dao

import app.data.local.source.SourceDatabase
import app.data.local.source.Wind_turbine
import app.core.model.WindTurbine

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

class SqlDelightWindTurbineDao(private val database: SourceDatabase) : WindTurbineDao {
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
