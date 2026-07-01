package app.data.local.dao

import app.data.local.entity.WindParkEntity

interface WindParkDao {
    suspend fun getAll(): List<WindParkEntity>
    suspend fun getById(id: String): WindParkEntity?
    suspend fun getByIds(ids: Collection<String>): List<WindParkEntity>
    suspend fun search(query: String): List<WindParkEntity>
    suspend fun insertOrReplace(entity: WindParkEntity)
    suspend fun getByMunicipality(municipalityId: String): List<WindParkEntity>
    suspend fun getByDistrict(districtId: String): List<WindParkEntity>
    suspend fun getByState(stateId: String): List<WindParkEntity>
}

