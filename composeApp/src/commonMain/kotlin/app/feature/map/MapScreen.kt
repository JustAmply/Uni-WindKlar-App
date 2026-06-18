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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import app.core.model.WindPark
import app.core.ui.components.PlatformMapView
import app.feature.report.ReportWindTurbineDialog
import kotlinx.coroutines.launch
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.start_background
import org.jetbrains.compose.resources.painterResource
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onParkSelected: (parkId: String) -> Unit,
) {
    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showReportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF7)),
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2D5A2D))
            }
        } else {
            PlatformMapView(
                centerLat = uiState.mapCenterLat,
                centerLon = uiState.mapCenterLon,
                zoomLevel = uiState.zoomLevel,
                parks = uiState.filteredParks,
                selectedParkId = uiState.selectedPark?.id,
                onMapMoved = viewModel::onMapMoved,
                onParkClicked = viewModel::onParkClickedById,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Components (Search Bar, Status Chips)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onQueryChange,
            )
            
            // Search results dropdown overlay
            if (uiState.showSearchOverlay && uiState.searchResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(uiState.searchResults) { park ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onSearchResultSelected(park) }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF2D5A2D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = park.name,
                                        color = Color(0xFF1A3A1A),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = park.municipalityName,
                                        color = Color(0xFF5A7A5A),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Aktiv", "Geplant", "Im Bau", "Stillgelegt").forEach { status ->
                    StatusChip(
                        text = status,
                        selected = uiState.selectedStatus == status,
                        onClick = { viewModel.setStatusFilter(status) }
                    )
                }
            }
        }

        // Floating Action Buttons (Zoom and Location controls)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Report wind turbine
            MapActionButton(
                icon = Icons.Outlined.Edit,
                contentDescription = "Windanlage melden",
                containerColor = Color(0xFF2D5A2D),
                contentColor = Color.White,
                onClick = { showReportDialog = true }
            )
            
            // Center location (Mocked to Leipzig/Leipzig Region)
            MapActionButton(
                icon = Icons.Outlined.NearMe,
                contentDescription = "Standort zentrieren",
                containerColor = Color.White,
                contentColor = Color(0xFF2D5A2D),
                onClick = {
                    viewModel.centerOnLocation(51.3397, 12.3731) // Center Leipzig
                    scope.launch {
                        snackbarHostState.showSnackbar("Karte zentriert auf Leipzig (Mock-Standort)")
                    }
                }
            )

            // Zoom in
            MapActionButton(
                icon = Icons.Outlined.Add,
                contentDescription = "Vergrößern",
                containerColor = Color.White,
                contentColor = Color(0xFF2D5A2D),
                onClick = { viewModel.onZoomChanged(uiState.zoomLevel + 1.0f) }
            )

            // Zoom out
            MapActionButton(
                icon = Icons.Outlined.Remove,
                contentDescription = "Verkleinern",
                containerColor = Color.White,
                contentColor = Color(0xFF2D5A2D),
                onClick = { viewModel.onZoomChanged(uiState.zoomLevel - 1.0f) }
            )
        }

        // Selected Park Preview Card
        uiState.selectedPark?.let { park ->
            val annualMetric = uiState.selectedParkMetrics.firstOrNull { it.metricType == "annual_production" }
            val co2Metric = uiState.selectedParkMetrics.firstOrNull { it.metricType == "co2_savings" }
            
            val prodStr = annualMetric?.value?.let { "${(it / 1_000_000.0).roundTo(1)} GWh" } ?: "k.A."
            val co2Str = co2Metric?.value?.let { "${formatNumber((it / 1000.0).toInt())} t" } ?: "k.A."

            ParkPreviewSheet(
                modifier = Modifier.align(Alignment.BottomCenter),
                parkName = park.name,
                municipalityName = park.municipalityName,
                productionVal = prodStr,
                co2Val = co2Str,
                sheetState = uiState.previewSheetState,
                onDetailsClick = { onParkSelected(park.id) },
                onExpand = viewModel::expandPreview,
                onMinimize = viewModel::minimizePreview,
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
        val reportPark = uiState.selectedPark
        val reportLatitude = reportPark?.latitude ?: uiState.mapCenterLat
        val reportLongitude = reportPark?.longitude ?: uiState.mapCenterLon

        ReportWindTurbineDialog(
            currentLatitude = reportLatitude,
            currentLongitude = reportLongitude,
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
    onQueryChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Windpark oder Ort suchen...",
                    color = Color(0xFF5A7A5A),
                    fontSize = 16.sp,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Suche",
                    tint = Color(0xFF5A7A5A),
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Color(0xFF2D5A2D) else Color.White,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            color = if (selected) Color.White else Color(0xFF2D5A2D),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MapActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
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

@Composable
private fun ParkPreviewSheet(
    modifier: Modifier = Modifier,
    parkName: String,
    municipalityName: String,
    productionVal: String,
    co2Val: String,
    sheetState: ParkPreviewSheetState,
    onDetailsClick: () -> Unit,
    onExpand: () -> Unit,
    onMinimize: () -> Unit,
) {
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = if (isDragging) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "parkPreviewOffsetY",
    )
    val sheetScaleY = 1f + ((-animatedOffsetY).coerceAtLeast(0f) / 700f)
    val sheetOffsetY = animatedOffsetY.coerceAtLeast(0f)

    LaunchedEffect(sheetState) {
        isDragging = false
        dragOffsetY = 0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(x = 0, y = sheetOffsetY.roundToInt()) }
            .graphicsLayer {
                scaleY = sheetScaleY
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clickable(
                enabled = sheetState == ParkPreviewSheetState.Minimized,
                onClick = onExpand,
            )
            .pointerInput(sheetState) {
                val minimizeThresholdPx = 96.dp.toPx()
                val expandThresholdPx = 44.dp.toPx()
                val maxExpandedPullPx = 34.dp.toPx()
                val maxMinimizedPullPx = 82.dp.toPx()

                detectVerticalDragGestures(
                    onDragEnd = {
                        isDragging = false
                        when {
                            sheetState == ParkPreviewSheetState.Expanded &&
                                dragOffsetY > minimizeThresholdPx -> onMinimize()

                            sheetState == ParkPreviewSheetState.Minimized &&
                                dragOffsetY < -expandThresholdPx -> onExpand()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onDragStart = {
                        isDragging = true
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY = when (sheetState) {
                            ParkPreviewSheetState.Expanded -> {
                                val nextOffset = dragOffsetY + dragAmount
                                if (nextOffset < 0f) {
                                    (nextOffset * 0.38f).coerceAtLeast(-maxExpandedPullPx)
                                } else {
                                    (nextOffset * 0.9f).coerceAtMost(minimizeThresholdPx * 1.35f)
                                }
                            }

                            ParkPreviewSheetState.Minimized -> {
                                val nextOffset = dragOffsetY + dragAmount
                                if (nextOffset < 0f) {
                                    (nextOffset * 0.62f).coerceAtLeast(-maxMinimizedPullPx)
                                } else {
                                    (nextOffset * 0.28f).coerceAtMost(18.dp.toPx())
                                }
                            }
                        }
                    },
                )
            },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 24.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(
                if (sheetState == ParkPreviewSheetState.Expanded) 16.dp else 10.dp
            ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD8E7D8)),
            )

            if (sheetState == ParkPreviewSheetState.Minimized) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = parkName,
                            color = Color(0xFF1A3A1A),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF5A7A5A),
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Gemeinde $municipalityName",
                                color = Color(0xFF5A7A5A),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9),
                    ) {
                        Text(
                            text = "Öffnen",
                            color = Color(0xFF2D5A2D),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(Res.drawable.start_background),
                        contentDescription = "Windpark Vorschau",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = CircleShape,
                        color = Color(0xFF43A047),
                    ) {
                        Text(
                            text = "Aktiv",
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = parkName,
                        color = Color(0xFF1A3A1A),
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF5A7A5A),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Gemeinde $municipalityName",
                            color = Color(0xFF5A7A5A),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Jahresproduktion",
                        value = productionVal,
                        icon = Icons.Outlined.Bolt,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "CO2 Einsparung",
                        value = co2Val,
                        icon = Icons.Outlined.Eco,
                    )
                }

                Button(
                    onClick = onDetailsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D5A2D),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Details anzeigen",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFE8F5E9),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2D5A2D),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    color = Color(0xFF2D5A2D),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Text(
                text = value,
                color = Color(0xFF1A3A1A),
                fontSize = 18.sp,
                lineHeight = 28.sp,
            )
        }
    }
}

private fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

private fun formatNumber(number: Int): String {
    return number.toString().reversed().chunked(3).joinToString(".").reversed()
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
