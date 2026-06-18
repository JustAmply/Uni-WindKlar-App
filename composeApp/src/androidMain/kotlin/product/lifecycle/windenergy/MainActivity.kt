package product.lifecycle.windenergy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.App
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.data.local.db.AppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val driver = AndroidSqliteDriver(AppDatabase.Schema, applicationContext, "windklar.db")
        val database = AppDatabase(driver)

        setContent {
            App(database)
        }
    }
}

