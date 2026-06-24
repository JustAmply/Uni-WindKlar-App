package product.lifecycle.windenergy

import app.App
import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.data.local.db.AppDatabase
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val driver = NativeSqliteDriver(AppDatabase.Schema, "windklar.db")
    try {
        driver.execute(null, "PRAGMA journal_mode = WAL;", 0)
        driver.execute(null, "PRAGMA synchronous = NORMAL;", 0)
        driver.execute(null, "PRAGMA temp_store = MEMORY;", 0)
        driver.execute(null, "PRAGMA cache_size = -64000;", 0)
        println("MainViewController: Successfully applied SQLite performance PRAGMAs on iOS.")
    } catch (e: Exception) {
        println("MainViewController ERROR: Failed to apply PRAGMAs on iOS: ${e.message}")
    }
    val database = AppDatabase(driver)
    val locationProvider = app.core.location.IosLocationProvider()
    return ComposeUIViewController { App(database, locationProvider) }
}
