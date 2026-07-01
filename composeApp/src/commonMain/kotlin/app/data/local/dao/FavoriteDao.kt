package app.data.local.dao

import app.data.local.user.UserDatabase

data class FavoriteRegionEntity(
    val regionType: String,
    val regionId: String,
    val createdAtEpochMillis: Long,
)

interface FavoriteDao {
    suspend fun getFavoriteIds(): List<String>
    suspend fun isFavorite(parkId: String): Boolean
    suspend fun addFavorite(parkId: String, timestamp: Long)
    suspend fun removeFavorite(parkId: String)

    suspend fun getFavoriteRegions(): List<FavoriteRegionEntity>
    suspend fun isRegionFavorite(type: String, id: String): Boolean
    suspend fun addRegionFavorite(type: String, id: String, timestamp: Long)
    suspend fun removeRegionFavorite(type: String, id: String)
}

class SqlDelightFavoriteDao(private val database: UserDatabase) : FavoriteDao {
    override suspend fun getFavoriteIds(): List<String> {
        return database.favoriteQueries.selectFavoriteIds().executeAsList()
    }

    override suspend fun isFavorite(parkId: String): Boolean {
        return database.favoriteQueries.isFavorite(parkId).executeAsOne()
    }

    override suspend fun addFavorite(parkId: String, timestamp: Long) {
        database.favoriteQueries.addFavorite(parkId, timestamp)
        database.favoriteQueries.updateFavoriteTimestamp(timestamp, parkId)
    }

    override suspend fun removeFavorite(parkId: String) {
        database.favoriteQueries.removeFavorite(parkId)
    }

    override suspend fun getFavoriteRegions(): List<FavoriteRegionEntity> {
        return database.favoriteQueries.selectFavoriteRegions().executeAsList().map {
            FavoriteRegionEntity(
                regionType = it.region_type,
                regionId = it.region_id,
                createdAtEpochMillis = it.created_at_epoch_millis
            )
        }
    }

    override suspend fun isRegionFavorite(type: String, id: String): Boolean {
        return database.favoriteQueries.isRegionFavorite(type, id).executeAsOne()
    }

    override suspend fun addRegionFavorite(type: String, id: String, timestamp: Long) {
        database.favoriteQueries.addRegionFavorite(type, id, timestamp)
        database.favoriteQueries.updateRegionFavoriteTimestamp(timestamp, type, id)
    }

    override suspend fun removeRegionFavorite(type: String, id: String) {
        database.favoriteQueries.removeRegionFavorite(type, id)
    }
}
