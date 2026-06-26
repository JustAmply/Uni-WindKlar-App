package app.feature.map

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import app.core.model.WindPark
import app.core.ui.components.EntityPreviewSheet
import app.core.ui.components.EntityType
import app.core.ui.components.PlatformMapView
import app.core.ui.components.PreviewSheetState
import app.core.ui.components.rememberLocationPermissionLauncher
import app.core.ui.theme.WindklarTheme
import app.feature.report.DataHintDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.start_background
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ScreenBackground @Composable get() = WindklarTheme.colors.screenBackground
private val PrimaryGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val HeaderEndGreen @Composable get() = WindklarTheme.colors.headerEndGreen
private val DarkGreen @Composable get() = WindklarTheme.colors.darkGreen
private val MutedGreen @Composable get() = WindklarTheme.colors.mutedGreen
private val PaleGreen @Composable get() = WindklarTheme.colors.paleGreen
private val LightOverlayGreen @Composable get() = WindklarTheme.colors.lightOverlayGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onParkSelected: (parkId: String) -> Unit,
    onRegionSelected: (type: String, id: String) -> Unit,
) {
    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showReportDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var reportedLatitude by remember { mutableStateOf(0.0) }
    var reportedLongitude by remember { mutableStateOf(0.0) }

    val permissionLauncher = rememberLocationPermissionLauncher { granted ->
        if (granted) {
            viewModel.centerOnUserLocation(
                onPermissionRequired = {},
                onError = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Standortberechtigung wurde abgelehnt.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else {
            PlatformMapView(
                centerLat = uiState.mapCenterLat,
                centerLon = uiState.mapCenterLon,
                zoomLevel = uiState.zoomLevel,
                markers = uiState.mapMarkers,
                selectedParkId = uiState.selectedPark?.id,
                onMapMoved = { lat, lon, zoom ->
                    focusManager.clearFocus()
                    viewModel.onMapMoved(lat, lon, zoom)
                },
                onMapMovedWithBounds = { lat, lon, zoom, swLat, swLon, neLat, neLon ->
                    focusManager.clearFocus()
                    viewModel.onMapMovedWithBounds(lat, lon, zoom, swLat, swLon, neLat, neLon)
                },
                onParkClicked = { parkId ->
                    focusManager.clearFocus()
                    viewModel.onParkClickedById(parkId)
                },
                onClusterClicked = { lat, lon ->
                    focusManager.clearFocus()
                    viewModel.onClusterClicked(lat, lon)
                },
                onPlacementPinDragged = viewModel::updatePlacementPinLocation,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Components (Search Bar and Search Results)
        if (!uiState.isPinPlacementMode) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    filters = uiState.filters,
                    onQueryChange = viewModel::onQueryChange,
                    onFocusChanged = viewModel::onSearchFocusChanged,
                    onFilterClick = {
                        focusManager.clearFocus()
                        showFilterSheet = true
                    },
                )
                
                // Search results dropdown overlay
                if (uiState.showSearchOverlay) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = WindklarTheme.colors.cardBackground,
                        shadowElevation = 8.dp
                    ) {
                        if (uiState.searchQuery.length < 2) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Zuletzt angesehen",
                                    color = WindklarTheme.colors.mutedGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                LazyColumn {
                                    items(uiState.recentParks, key = { it.id }) { park ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    focusManager.clearFocus()
                                                    viewModel.onSearchResultSelected(MapSearchResult.Park(park))
                                                }
                                                .padding(vertical = 8.dp, horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = WindklarTheme.colors.primaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = park.name,
                                                    color = WindklarTheme.colors.darkGreen,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${park.municipalityName} • ${formatWindInstallationCount(park.turbineCount)}",
                                                    color = WindklarTheme.colors.mutedGreen,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (uiState.searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Keine Ergebnisse gefunden",
                                        color = MutedGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                LazyColumn(modifier = Modifier.padding(8.dp)) {
                                    items(uiState.searchResults, key = { it.key }) { result ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    focusManager.clearFocus()
                                                    viewModel.onSearchResultSelected(result)
                                                }
                                                .padding(vertical = 8.dp, horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = WindklarTheme.colors.primaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            
                                             val title: String
                                             val subtitle: String
                                             
                                             when (result) {
                                                 is MapSearchResult.State -> {
                                                     title = result.name
                                                     subtitle = "Bundesland"
                                                 }
                                                 is MapSearchResult.District -> {
                                                     title = result.name
                                                     subtitle = "Landkreis in ${result.stateName}"
                                                 }
                                                 is MapSearchResult.Municipality -> {
                                                     title = result.name
                                                     subtitle = "Gemeinde in ${result.stateName}"
                                                 }
                                                 is MapSearchResult.Park -> {
                                                     title = result.park.name
                                                     subtitle = "${result.park.municipalityName} • ${formatWindInstallationCount(result.park.turbineCount)}"
                                                 }
                                             }
                                             
                                             Column(modifier = Modifier.weight(1f)) {
                                                 Text(
                                                     text = title,
                                                     color = WindklarTheme.colors.darkGreen,
                                                     fontWeight = FontWeight.Medium,
                                                     fontSize = 14.sp
                                                 )
                                                 Text(
                                                     text = subtitle,
                                                     color = WindklarTheme.colors.mutedGreen,
                                                     fontSize = 12.sp
                                                 )
                                             }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

        // Location stays visible; zoom is handled through map gestures on mobile.
        if (!uiState.isPinPlacementMode) {
            val locationButtonBottomPadding = locationButtonBottomPadding(
                hasPreview = uiState.selectedPreviewData != null,
                previewSheetState = uiState.previewSheetState,
            )

            MapActionButton(
                icon = Icons.Outlined.NearMe,
                contentDescription = "Standort zentrieren",
                containerColor = WindklarTheme.colors.cardBackground,
                contentColor = PrimaryGreen,
                onClick = {
                    viewModel.centerOnUserLocation(
                        onPermissionRequired = {
                            permissionLauncher()
                        },
                        onError = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = locationButtonBottomPadding),
            )
        }

        // Selected wind park or region preview card
        if (!uiState.isPinPlacementMode) {
            uiState.selectedPreviewData?.let { previewData ->
                EntityPreviewSheet(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    previewData = previewData,
                    sheetState = uiState.previewSheetState,
                    onDetailsClick = {
                        when (previewData.type) {
                            EntityType.PARK -> onParkSelected(previewData.id)
                            EntityType.STATE -> onRegionSelected("state", previewData.id)
                            EntityType.DISTRICT -> onRegionSelected("district", previewData.id)
                            EntityType.CITY -> onRegionSelected("city", previewData.id)
                        }
                    },
                    onExpand = viewModel::expandPreview,
                    onMinimize = viewModel::minimizePreview,
                )
            }
        }

        // Pin Placement Mode Bottom Card
        if (uiState.isPinPlacementMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = WindklarTheme.colors.cardBackground,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Standort festlegen",
                        color = DarkGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verschieben Sie den roten Pin auf der Karte zum genauen Standort der Windanlage.",
                        color = MutedGreen,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.cancelPinPlacement() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaleGreen,
                                contentColor = PrimaryGreen
                             )
                        ) {
                            Text("Abbrechen", fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = {
                                viewModel.confirmPinPlacement { lat, lon ->
                                    reportedLatitude = lat
                                    reportedLongitude = lon
                                    showReportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Auswählen", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        if (showFilterSheet && !uiState.isPinPlacementMode) {
            MapFilterSheet(
                filters = uiState.filters,
                onStatusSelected = viewModel::setStatusFilter,
                onIncludeDecommissionedChanged = viewModel::setIncludeDecommissioned,
                onSizeRangeSelected = viewModel::setParkSizeRange,
                onCapacityRangeSelected = viewModel::setCapacityRange,
                onReset = viewModel::resetMapFilters,
                onDismiss = { showFilterSheet = false },
            )
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }

    // Report Dialog
    if (showReportDialog) {
        val reportPark = uiState.pendingReportPark ?: uiState.selectedPark
        val reportLatitude = reportedLatitude
        val reportLongitude = reportedLongitude

        DataHintDialog(
            latitude = reportLatitude,
            longitude = reportLongitude,
            contextLabel = reportPark?.name?.takeIf { it.isNotBlank() }?.let { "Windpark $it" }
                ?: "manuell gesetzter Pin",
            defaultCategory = "missing_installation",
            onDismiss = { showReportDialog = false },
            onSubmit = { category, confidence, description, suggestedValue ->
                viewModel.submitDataHint(
                    category = category,
                    confidence = confidence,
                    description = description,
                    suggestedValue = suggestedValue,
                    latitude = reportLatitude,
                    longitude = reportLongitude,
                    windParkId = reportPark?.id,
                    municipalityId = reportPark?.municipalityId,
                    onSuccess = {
                        showReportDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Vielen Dank! Ihr Datenhinweis wurde lokal gespeichert.")
                        }
                    }
                )
            }
        )
    }

}

@Composable
private fun SearchBar(
    query: String,
    filters: MapFilterState,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 6.dp,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                },
            placeholder = {
                Text(
                    text = "Windpark oder Gemeinde suchen...",
                    color = MutedGreen,
                    fontSize = 15.sp,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = PrimaryGreen,
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Suche leeren",
                                tint = PrimaryGreen,
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = onFilterClick) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = "Kartenfilter öffnen",
                                tint = if (filters.activeFilterCount > 0) PrimaryGreen else MutedGreen,
                            )
                        }
                        if (filters.activeFilterCount > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(18.dp),
                                shape = CircleShape,
                                color = PrimaryGreen,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = filters.activeFilterCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = WindklarTheme.colors.cardBackground,
                unfocusedContainerColor = WindklarTheme.colors.cardBackground,
                disabledContainerColor = WindklarTheme.colors.cardBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun MapFilterSheet(
    filters: MapFilterState,
    onStatusSelected: (MapStatusFilter) -> Unit,
    onIncludeDecommissionedChanged: (Boolean) -> Unit,
    onSizeRangeSelected: (MapParkSizeRange) -> Unit,
    onCapacityRangeSelected: (MapCapacityRange) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.24f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {}),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = WindklarTheme.colors.cardBackground,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Kartenfilter",
                        color = DarkGreen,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (filters.activeFilterCount > 0) {
                            Surface(
                                onClick = onReset,
                                shape = CircleShape,
                                color = PaleGreen,
                            ) {
                                Text(
                                    text = "Zurücksetzen",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = PrimaryGreen,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kartenfilter schließen",
                                tint = MutedGreen,
                            )
                        }
                    }
                }

                FilterChoiceSection(
                    title = "Status",
                    options = MapStatusFilter.values().toList(),
                    selected = filters.status,
                    label = { it.label },
                    onSelected = onStatusSelected,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weitere",
                            color = MutedGreen,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Stillgelegte anzeigen",
                            color = DarkGreen,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Switch(
                        checked = filters.includeDecommissioned,
                        onCheckedChange = onIncludeDecommissionedChanged,
                    )
                }

                FilterChoiceSection(
                    title = "Parkgröße",
                    options = MapParkSizeRange.values().toList(),
                    selected = filters.sizeRange,
                    label = { it.label },
                    onSelected = onSizeRangeSelected,
                )

                FilterChoiceSection(
                    title = "Leistung",
                    options = MapCapacityRange.values().toList(),
                    selected = filters.capacityRange,
                    label = { it.label },
                    onSelected = onCapacityRangeSelected,
                )
            }
        }
    }
}

@Composable
private fun <T> FilterChoiceSection(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MutedGreen,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChoiceChip(
                    text = label(option),
                    selected = option == selected,
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun FilterChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) WindklarTheme.colors.primaryGreen else Color.White,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            color = if (selected) Color.White else WindklarTheme.colors.primaryGreen,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun locationButtonBottomPadding(
    hasPreview: Boolean,
    previewSheetState: PreviewSheetState,
): Dp =
    if (!hasPreview) {
        24.dp
    } else {
        when (previewSheetState) {
            PreviewSheetState.Expanded -> 332.dp
            PreviewSheetState.Minimized -> 116.dp
        }
    }

private fun formatWindInstallationCount(count: Int): String =
    "$count Windanlage${if (count == 1) "" else "n"}"

@Composable
private fun MapActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 10.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}




private val germanyBorderPoints = listOf(
    // Coordinates as Offset(Longitude, Latitude)
    Offset(8.4f, 55.05f),
    Offset(8.6f, 54.9f),
    Offset(9.4f, 54.8f),
    Offset(10.1f, 54.3f),
    Offset(11.2f, 54.5f),
    Offset(12.1f, 54.2f),
    Offset(13.4f, 54.6f),
    Offset(14.2f, 53.9f),
    Offset(14.3f, 53.3f),
    Offset(14.6f, 52.3f),
    Offset(14.7f, 51.9f),
    Offset(15.0f, 51.15f),
    Offset(14.8f, 50.8f),
    Offset(14.2f, 50.9f),
    Offset(12.5f, 50.3f),
    Offset(12.1f, 50.3f),
    Offset(12.2f, 50.1f),
    Offset(12.4f, 50.0f),
    Offset(12.8f, 49.3f),
    Offset(13.5f, 48.6f),
    Offset(13.0f, 48.2f),
    Offset(13.0f, 47.8f),
    Offset(13.0f, 47.6f),
    Offset(12.2f, 47.6f),
    Offset(11.3f, 47.4f),
    Offset(10.2f, 47.27f),
    Offset(9.7f, 47.5f),
    Offset(9.2f, 47.6f),
    Offset(8.2f, 47.6f),
    Offset(7.6f, 47.55f),
    Offset(7.6f, 48.0f),
    Offset(7.8f, 48.6f),
    Offset(8.2f, 49.0f),
    Offset(8.2f, 49.0f),
    Offset(7.3f, 49.2f),
    Offset(6.4f, 49.5f),
    Offset(6.4f, 49.8f),
    Offset(6.1f, 50.0f),
    Offset(6.2f, 50.5f),
    Offset(6.1f, 50.8f),
    Offset(6.0f, 51.1f),
    Offset(6.2f, 51.3f),
    Offset(6.2f, 51.8f),
    Offset(6.8f, 51.9f),
    Offset(7.0f, 52.2f),
    Offset(7.2f, 53.2f),
    Offset(7.0f, 53.3f),
    Offset(8.1f, 53.5f),
    Offset(8.7f, 53.9f),
    Offset(8.6f, 54.3f),
    Offset(9.0f, 54.5f)
)
