package app.data.local.dao

import app.data.local.user.UserDatabase

interface SettingsDao {
    suspend fun getValue(key: String): String?
    suspend fun upsertValue(key: String, value: String)
}

class SqlDelightSettingsDao(private val database: UserDatabase) : SettingsDao {
    override suspend fun getValue(key: String): String? {
        return database.settingQueries.getSetting(key).executeAsOneOrNull()
    }

    override suspend fun upsertValue(key: String, value: String) {
        database.settingQueries.upsertSetting(key, value)
        database.settingQueries.updateSetting(value, key)
    }
}
