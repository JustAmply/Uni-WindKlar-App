package app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector
import app.core.ui.components.WindKlarBottomNav
import app.core.ui.components.WindKlarBottomNavItem
import app.feature.detail.ParkDetailScreen
import app.feature.favorites.FavoritesScreen
import app.feature.faq.FaqScreen
import app.feature.map.MapScreen
import app.feature.profile.ProfileScreen
import app.feature.start.StartScreen
import app.feature.stats.StatsScreen

@Composable
fun AppNavHost() {
    var currentRoute: Route by remember { mutableStateOf(Route.Start) }

    Scaffold(
        bottomBar = {
            if (currentRoute.isTopLevelRoute()) {
                WindKlarBottomNav(
                    items = topLevelRoutes.map { route ->
                        val isSelected = currentRoute.isSameTopLevelRoute(route)
                        WindKlarBottomNavItem(
                            label = route.title,
                            icon = route.navIcon(selected = isSelected),
                            selected = isSelected,
                            onClick = { currentRoute = route },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val route = currentRoute) {
                Route.Start -> StartScreen(
                    onGetStartedClick = { currentRoute = Route.Map },
                )

                Route.Map -> MapScreen(
                    onParkSelected = { parkId -> currentRoute = Route.Detail(parkId) },
                )

                Route.Stats -> StatsScreen(
                    onBackClick = { currentRoute = Route.Map },
                )

                Route.Favorites -> FavoritesScreen(
                    onBackClick = { currentRoute = Route.Map },
                    onParkSelected = { parkId -> currentRoute = Route.Detail(parkId) },
                )

                Route.Faq -> FaqScreen()
                Route.Profile -> ProfileScreen()
                is Route.Detail -> ParkDetailScreen(
                    parkId = route.parkId,
                    onBack = { currentRoute = Route.Map },
                )
            }
        }
    }
}

private fun Route.isSameTopLevelRoute(other: Route): Boolean =
    this::class == other::class

private fun Route.isTopLevelRoute(): Boolean =
    this is Route.Map ||
        this is Route.Stats ||
        this is Route.Favorites ||
        this is Route.Faq ||
        this is Route.Profile

private fun Route.navIcon(selected: Boolean): ImageVector = when (this) {
    Route.Map -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
    Route.Stats -> if (selected) Icons.Filled.QueryStats else Icons.Outlined.QueryStats
    Route.Favorites -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    Route.Faq -> if (selected) Icons.AutoMirrored.Filled.Help else Icons.AutoMirrored.Outlined.HelpOutline
    Route.Profile -> if (selected) Icons.Filled.Person else Icons.Outlined.PersonOutline
    Route.Start -> Icons.Outlined.Map
    is Route.Detail -> Icons.Outlined.Map
}
