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
import java.io.File

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
                    db.enableWriteAheadLogging()
                    runCatching {
                        db.query("PRAGMA synchronous = NORMAL").close()
                        db.query("PRAGMA temp_store = MEMORY").close()
                        db.query("PRAGMA cache_size = -64000").close()
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
        if (targetFile.exists()) return
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
}
