package app

import androidx.compose.runtime.Composable
import app.core.ui.theme.WindklarTheme
import app.data.local.db.AppDatabase
import app.navigation.AppNavHost

@Composable
fun App(database: AppDatabase) {
    WindklarTheme {
        AppNavHost(database)
    }
}

