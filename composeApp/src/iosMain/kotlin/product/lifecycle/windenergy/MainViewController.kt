package product.lifecycle.windenergy

import app.App
import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.data.local.db.AppDatabase

fun MainViewController(): androidx.compose.ui.ViewController {
    val driver = NativeSqliteDriver(AppDatabase.Schema, "windklar.db")
    val database = AppDatabase(driver)
    return ComposeUIViewController { App(database) }
}

