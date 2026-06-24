package product.lifecycle.windenergy

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.App
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.data.local.db.AppDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val databaseName = "windklar.db"
        preseedDatabaseFromAssets(applicationContext, databaseName)
        val driver = AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = applicationContext,
            name = databaseName,
            callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                    db.enableWriteAheadLogging()
                    runCatching {
                        db.execSQL("PRAGMA synchronous = NORMAL")
                        db.execSQL("PRAGMA temp_store = MEMORY")
                        db.execSQL("PRAGMA cache_size = -64000")
                    }.onFailure { error ->
                        println("MainActivity: Failed to apply performance PRAGMAs: ${error.message}")
                    }
                }
            }
        )
        val database = AppDatabase(driver)

        val locationProvider = app.core.location.AndroidLocationProvider(applicationContext)

        setContent {
            App(database, locationProvider)
        }
    }

    private fun preseedDatabaseFromAssets(context: Context, databaseName: String) {
        val targetFile = context.getDatabasePath(databaseName)
        
        if (targetFile.exists()) {
            val isReadableSnapshotDatabase = runCatching {
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    targetFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                ).use { db ->
                    val hasRequiredTables = db.hasTable("wind_park") &&
                        db.hasTable("wind_turbine") &&
                        db.hasTable("metric") &&
                        db.hasTable("snapshot_metadata") &&
                        db.hasTable("app_setting")
                    hasRequiredTables &&
                        db.countRows("wind_park") > 0L &&
                        db.countRows("wind_turbine") > 0L &&
                        db.countRows("metric") > 0L
                }
            }.onFailure { error ->
                println("MainActivity: Failed to validate existing database: ${error.message}")
            }.getOrDefault(false)
            
            if (isReadableSnapshotDatabase) {
                println("MainActivity: Existing database is readable. Importer will repair stale snapshot content if needed.")
                return
            }
            
            println("MainActivity: Existing database is not a readable WindKlar snapshot DB. Replacing it with bundled preseed...")
            runCatching {
                context.deleteDatabase(databaseName)
            }.onFailure { error ->
                println("MainActivity: Failed to delete old database: ${error.message}")
            }
        }
        
        runCatching {
            context.assets.open("windklar_seed.db").use { input ->
                targetFile.parentFile?.mkdirs()
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("MainActivity: Copied preseed database to ${targetFile.absolutePath}")
        }.onFailure { error ->
            println("MainActivity: No preseed database available, falling back to JSON import: ${error.message}")
        }
    }

    private fun android.database.sqlite.SQLiteDatabase.hasTable(tableName: String): Boolean =
        rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        ).use { cursor -> cursor.moveToFirst() }

    private fun android.database.sqlite.SQLiteDatabase.countRows(tableName: String): Long =
        rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

}
