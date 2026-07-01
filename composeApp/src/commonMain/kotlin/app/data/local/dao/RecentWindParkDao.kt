package app.data.local.dao

import app.data.local.user.UserDatabase

interface RecentWindParkDao {
    suspend fun getRecentWindParkIds(limit: Long): List<String>
    suspend fun recordRecentWindPark(parkId: String, timestamp: Long)
    suspend fun clear()
}

class SqlDelightRecentWindParkDao(private val database: UserDatabase) : RecentWindParkDao {
    override suspend fun getRecentWindParkIds(limit: Long): List<String> {
        return database.recentWindParkQueries.selectRecentWindParks(limit).executeAsList()
    }

    override suspend fun recordRecentWindPark(parkId: String, timestamp: Long) {
        database.recentWindParkQueries.recordRecentWindPark(parkId, timestamp)
        database.recentWindParkQueries.updateRecentWindParkTimestamp(timestamp, parkId)
    }

    override suspend fun clear() {
        database.recentWindParkQueries.clearRecentWindParks()
    }
}
