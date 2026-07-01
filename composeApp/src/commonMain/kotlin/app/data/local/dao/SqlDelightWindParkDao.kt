package app.data.local.dao

import app.data.local.source.SourceDatabase
import app.data.local.source.Wind_park
import app.data.local.entity.WindParkEntity

class SqlDelightWindParkDao(
    private val database: SourceDatabase,
) : WindParkDao {
    override suspend fun getAll(): List<WindParkEntity> {
        return database.windParkQueries.selectAllWindParks().executeAsList().map { it.toEntity() }
    }

    override suspend fun getById(id: String): WindParkEntity? {
        return database.windParkQueries.selectWindParkById(id).executeAsOneOrNull()?.toEntity()
    }
    
    override suspend fun getByIds(ids: Collection<String>): List<WindParkEntity> {
        if (ids.isEmpty()) return emptyList()
        return database.windParkQueries.selectWindParksByIds(ids).executeAsList().map { it.toEntity() }
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

    override suspend fun getByMunicipality(municipalityId: String): List<WindParkEntity> {
        return database.windParkQueries.selectWindParksByMunicipality(municipalityId).executeAsList().map { it.toEntity() }
    }

    override suspend fun getByDistrict(districtId: String): List<WindParkEntity> {
        return database.windParkQueries.selectWindParksByDistrict(districtId).executeAsList().map { it.toEntity() }
    }

    override suspend fun getByState(stateId: String): List<WindParkEntity> {
        return database.windParkQueries.selectWindParksByState(stateId).executeAsList().map { it.toEntity() }
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
