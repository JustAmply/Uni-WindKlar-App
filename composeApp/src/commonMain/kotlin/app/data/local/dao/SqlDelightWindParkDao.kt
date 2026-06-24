package app.data.local.dao

import app.data.local.db.AppDatabase
import app.data.local.db.Wind_park
import app.data.local.entity.WindParkEntity

class SqlDelightWindParkDao(
    private val database: AppDatabase,
) : WindParkDao {
    override suspend fun getAll(): List<WindParkEntity> {
        return database.windParkQueries.selectAllWindParks().executeAsList().map { it.toEntity() }
    }

    override suspend fun getById(id: String): WindParkEntity? {
        return database.windParkQueries.selectWindParkById(id).executeAsOneOrNull()?.toEntity()
    }

    override suspend fun search(query: String): List<WindParkEntity> {
        return database.windParkQueries.searchWindParks(query).executeAsList().map { it.toEntity() }
    }

    override suspend fun insertOrReplace(entity: WindParkEntity) {
        database.windParkQueries.upsertWindPark(
            id = entity.id,
            name = entity.name,
            municipality_id = entity.municipalityId,
            municipality_name = entity.municipalityName,
            district_id = entity.districtId,
            district_name = entity.districtName,
            state_id = entity.stateId,
            state_name = entity.stateName,
            latitude = entity.latitude,
            longitude = entity.longitude,
            turbine_count = entity.turbineCount?.toLong(),
            installed_capacity_kw = entity.installedCapacityKw,
            grouping_method = entity.groupingMethod,
            source_name = entity.sourceName,
            source_url = entity.sourceUrl,
            source_updated_at = entity.sourceUpdatedAt,
            data_quality = entity.dataQuality
        )
        database.windParkQueries.updateWindPark(
            id = entity.id,
            name = entity.name,
            municipality_id = entity.municipalityId,
            municipality_name = entity.municipalityName,
            district_id = entity.districtId,
            district_name = entity.districtName,
            state_id = entity.stateId,
            state_name = entity.stateName,
            latitude = entity.latitude,
            longitude = entity.longitude,
            turbine_count = entity.turbineCount?.toLong(),
            installed_capacity_kw = entity.installedCapacityKw,
            grouping_method = entity.groupingMethod,
            source_name = entity.sourceName,
            source_url = entity.sourceUrl,
            source_updated_at = entity.sourceUpdatedAt,
            data_quality = entity.dataQuality
        )
    }

    private fun Wind_park.toEntity() = WindParkEntity(
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
        turbineCount = turbine_count?.toInt(),
        installedCapacityKw = installed_capacity_kw,
        groupingMethod = grouping_method,
        sourceName = source_name,
        sourceUrl = source_url,
        sourceUpdatedAt = source_updated_at,
        dataQuality = data_quality
    )
}
