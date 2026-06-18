package app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector
import app.core.ui.components.WindKlarBottomNav
import app.core.ui.components.WindKlarBottomNavItem
import app.data.local.db.AppDatabase
import app.data.repository.SqlDelightWindParkRepository
import app.data.seed.SnapshotSeedDataImporter
import app.data.snapshot.ComposeResourceSnapshotProvider
import app.feature.detail.ParkDetailScreen
import app.feature.detail.ParkDetailViewModel
import app.feature.favorites.FavoritesScreen
import app.feature.favorites.FavoritesViewModel
import app.feature.faq.FaqScreen
import app.feature.map.MapScreen
import app.feature.map.MapViewModel
import app.feature.profile.ProfileScreen
import app.feature.profile.ProfileViewModel
import app.feature.start.StartScreen
import app.feature.stats.StatsScreen
import app.feature.stats.StatsViewModel

@Composable
fun AppNavHost(database: AppDatabase) {
    var isSeeded by remember { mutableStateOf(false) }
    var seedError by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(database, retryCount) {
        try {
            seedError = null
            println("AppNavHost: Starting database seeding (attempt ${retryCount + 1})...")
            val importer = SnapshotSeedDataImporter(
                database = database,
                snapshotProvider = ComposeResourceSnapshotProvider()
            )
            importer.importIfNeeded()
            println("AppNavHost: Database seeding succeeded!")
            isSeeded = true
        } catch (e: Throwable) {
            println("AppNavHost ERROR: Database seeding failed!")
            e.printStackTrace()
            seedError = e.message ?: e.toString()
        }
    }

    if (!isSeeded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAF7))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (seedError != null) {
                    Text(
                        text = "Fehler beim Laden",
                        color = Color(0xFFD32F2F),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Beim Laden der Windparkdaten ist ein Fehler aufgetreten:\n$seedError",
                        color = Color(0xFF5C1D1D),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { retryCount++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D5A2D))
                        ) {
                            Text("Erneut versuchen")
                        }
                        Button(
                            onClick = { 
                                println("AppNavHost: Seeding bypassed by user.")
                                isSeeded = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                        ) {
                            Text("Ohne Daten starten")
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        color = Color(0xFF2D5A2D),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "WindKlar",
                        color = Color(0xFF1A3A1A),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Windparkdaten werden geladen...",
                        color = Color(0xFF5A7A5A),
                        fontSize = 14.sp
                    )
                }
            }
        }
        return
    }

    val repository = remember(database) { SqlDelightWindParkRepository(database) }
    var currentRoute: Route by remember { mutableStateOf(Route.Start) }

    val mapViewModel = remember(repository) { MapViewModel(repository) }
    val favoritesViewModel = remember(repository) { FavoritesViewModel(repository) }
    val statsViewModel = remember(repository) { StatsViewModel(repository) }
    val profileViewModel = remember(repository) { ProfileViewModel(repository) }

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
                    viewModel = mapViewModel,
                    onParkSelected = { parkId -> currentRoute = Route.Detail(parkId) },
                )

                Route.Stats -> StatsScreen(
                    viewModel = statsViewModel,
                    onBackClick = { currentRoute = Route.Map },
                )

                Route.Favorites -> FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onBackClick = { currentRoute = Route.Map },
                    onParkSelected = { parkId -> currentRoute = Route.Detail(parkId) },
                )

                Route.Faq -> FaqScreen(
                    onBackClick = { currentRoute = Route.Map },
                )
                
                Route.Profile -> ProfileScreen(
                    viewModel = profileViewModel,
                    onBackClick = { currentRoute = Route.Map },
                )
                
                is Route.Detail -> {
                    val detailViewModel = remember(route.parkId) { ParkDetailViewModel(route.parkId, repository) }
                    ParkDetailScreen(
                        viewModel = detailViewModel,
                        onBack = { currentRoute = Route.Map },
                    )
                }
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
    Route.Profile -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    Route.Start -> Icons.Outlined.Map
    is Route.Detail -> Icons.Outlined.Map
}

