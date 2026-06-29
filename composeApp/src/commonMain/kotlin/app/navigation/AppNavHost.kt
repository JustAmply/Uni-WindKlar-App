package app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector
import app.AppGraph
import app.core.ui.components.WindKlarBottomNav
import app.core.ui.components.WindKlarBottomNavItem
import app.core.ui.theme.WindklarTheme
import app.feature.detail.ParkDetailScreen
import app.feature.detail.RegionDetailScreen
import app.feature.favorites.FavoritesScreen
import app.feature.faq.FaqScreen
import app.feature.map.MapScreen
import app.feature.profile.ProfileScreen
import app.feature.start.StartScreen
import app.feature.stats.ImpactDetailScreen
import app.feature.stats.StatsScreen
import app.feature.stats.toImpactDetailUiState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow

@Composable
fun AppNavHost(appGraph: AppGraph) {
    var startRoute by remember { mutableStateOf<Route?>(null) }

    LaunchedEffect(appGraph) {
        val completed = appGraph.repository.isOnboardingCompleted()
        startRoute = if (completed) Route.Map else Route.Start
    }

    val resolvedStartRoute = startRoute
    if (resolvedStartRoute == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WindklarTheme.colors.screenBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = WindklarTheme.colors.primaryGreen)
        }
        return
    }

    var currentRoute: Route by remember(resolvedStartRoute) { mutableStateOf(resolvedStartRoute) }
    var routeHistory by remember { mutableStateOf(listOf<Route>()) }
    val coroutineScope = rememberCoroutineScope()

    val navigateTo: (Route) -> Unit = { newRoute ->
        routeHistory = routeHistory + currentRoute
        currentRoute = newRoute
    }
    
    val navigateBack: () -> Unit = {
        if (routeHistory.isNotEmpty()) {
            currentRoute = routeHistory.last()
            routeHistory = routeHistory.dropLast(1)
        } else {
            currentRoute = Route.Map
        }
    }

    val navigateToCountry: () -> Unit = {
        routeHistory = emptyList()
        currentRoute = Route.Stats
    }

    val mapViewModel = remember(appGraph) { appGraph.mapViewModel() }
    val favoritesViewModel = remember(appGraph) { appGraph.favoritesViewModel() }
    val statsViewModel = remember(appGraph) { appGraph.statsViewModel() }
    val profileViewModel = remember(appGraph) { appGraph.profileViewModel() }

    LaunchedEffect(appGraph) {
        snapshotFlow { currentRoute }
            .filter { it != Route.Start }
            .first()
        snapshotFlow { mapViewModel.uiState.isLoading }
            .filter { !it }
            .first()

        launch { statsViewModel.loadIfNeeded() }
        launch { favoritesViewModel.loadData() }
    }

    Scaffold(
        contentWindowInsets = if (currentRoute == Route.Start) {
            WindowInsets(0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (currentRoute != Route.Start) {
                val activeTopLevelRoute = currentRoute.let {
                    if (it.isTopLevelRoute()) it
                    else routeHistory.lastOrNull { r -> r.isTopLevelRoute() } ?: Route.Map
                }
                WindKlarBottomNav(
                    items = topLevelRoutes.map { route ->
                        val isSelected = activeTopLevelRoute.isSameTopLevelRoute(route)
                        WindKlarBottomNavItem(
                            label = route.title,
                            icon = route.navIcon(selected = isSelected),
                            selected = isSelected,
                            onClick = {
                                if (isSelected && route is Route.Map) {
                                    mapViewModel.minimizePreview()
                                }
                                if (route is Route.Favorites) {
                                    favoritesViewModel.loadData(force = true)
                                }
                                routeHistory = emptyList()
                                currentRoute = route
                            },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (currentRoute != Route.Start) {
                MapScreen(
                    viewModel = mapViewModel,
                    onParkSelected = { parkId ->
                        navigateTo(Route.Detail(parkId))
                    },
                    onRegionSelected = { type, id ->
                        navigateTo(Route.RegionDetail(type, id))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f),
                    isVisible = currentRoute is Route.Map,
                )
            }

            if (currentRoute != Route.Map) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .background(WindklarTheme.colors.screenBackground),
                ) {
                    when (val route = currentRoute) {
                Route.Start -> StartScreen(
                    onGetStartedClick = {
                        coroutineScope.launch {
                            appGraph.repository.setOnboardingCompleted(true)
                        }
                        currentRoute = Route.Map
                    },
                )

                Route.Map -> Unit

                Route.Stats -> StatsScreen(
                    viewModel = statsViewModel,
                    onNavigateToParkDetail = { parkId ->
                        navigateTo(Route.Detail(parkId))
                    },
                    onNavigateToRegionDetail = { type, id ->
                        navigateTo(Route.RegionDetail(type, id))
                    },
                    onNavigateToImpactDetail = { metricType ->
                        navigateTo(Route.ImpactDetail(metricType))
                    },
                )

                Route.Favorites -> FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onParkSelected = { parkId ->
                        navigateTo(Route.Detail(parkId))
                    },
                    onRegionSelected = { type, id ->
                        navigateTo(Route.RegionDetail(type, id))
                    },
                )

                Route.Faq -> FaqScreen()
                
                Route.Profile -> ProfileScreen(
                    viewModel = profileViewModel,
                    onReplayOnboarding = {
                        navigateTo(Route.Start)
                    },
                    onCreateDataHint = {
                        routeHistory = emptyList()
                        currentRoute = Route.Map
                        mapViewModel.startPinPlacement(reportPark = null)
                    },
                )
                
                is Route.Detail -> {
                    val detailViewModel = remember(appGraph, route.parkId) {
                        appGraph.parkDetailViewModel(route.parkId)
                    }
                    ParkDetailScreen(
                        viewModel = detailViewModel,
                        onBack = { navigateBack() },
                        onNavigateToRegion = { type, id ->
                            navigateTo(Route.RegionDetail(type, id))
                        },
                        onNavigateToCountry = navigateToCountry,
                        onShowParkOnMap = {
                            mapViewModel.selectParkOnMap(route.parkId)
                            routeHistory = emptyList()
                            currentRoute = Route.Map
                        },
                        onShowTurbineOnMap = { turbineId ->
                            mapViewModel.selectTurbineOnMap(route.parkId, turbineId)
                            routeHistory = emptyList()
                            currentRoute = Route.Map
                        },
                    )
                }

                is Route.RegionDetail -> {
                    val regionViewModel = remember(appGraph, route.type, route.id) {
                        appGraph.regionDetailViewModel(route.type, route.id)
                    }
                    RegionDetailScreen(
                        viewModel = regionViewModel,
                        onBack = { navigateBack() },
                        onParkSelected = { parkId ->
                            navigateTo(Route.Detail(parkId))
                        },
                        onRegionSelected = { type, id ->
                            navigateTo(Route.RegionDetail(type, id))
                        },
                        onNavigateToCountry = navigateToCountry,
                        onShowRegionOnMap = {
                            mapViewModel.selectRegionOnMap(route.type, route.id)
                            routeHistory = emptyList()
                            currentRoute = Route.Map
                        },
                    )
                }

                is Route.ImpactDetail -> {
                    LaunchedEffect(route.metricType) {
                        statsViewModel.loadImpactDetail(route.metricType)
                    }
                    ImpactDetailScreen(
                        uiState = statsViewModel.uiState.toImpactDetailUiState(route.metricType),
                        onBack = { navigateBack() },
                        onNavigateToParkDetail = { parkId ->
                            navigateTo(Route.Detail(parkId))
                        },
                        onNavigateToRegionDetail = { type, id ->
                            navigateTo(Route.RegionDetail(type, id))
                        },
                    )
                }
            }
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
    Route.Profile -> if (selected) Icons.Filled.Info else Icons.Outlined.Info
    Route.Start -> Icons.Outlined.Map
    is Route.Detail -> Icons.Outlined.Map
    is Route.RegionDetail -> Icons.Outlined.Map
    is Route.ImpactDetail -> Icons.Outlined.QueryStats
}
