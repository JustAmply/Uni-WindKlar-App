package app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.core.location.LocationProvider
import app.core.ui.theme.WindklarTheme
import app.data.local.source.SourceDatabase
import app.data.local.user.UserDatabase
import app.navigation.AppNavHost

@Composable
fun App(
    sourceDatabase: SourceDatabase,
    userDatabase: UserDatabase,
    locationProvider: LocationProvider,
) {
    val appGraph = remember(sourceDatabase, userDatabase, locationProvider) {
        AppGraph(sourceDatabase, userDatabase, locationProvider)
    }

    WindklarTheme {
        AppNavHost(appGraph)
    }
}
