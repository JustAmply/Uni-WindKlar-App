package app.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.ui.theme.WindklarTheme
import app.core.util.formatGermanNumber
import kotlin.math.roundToInt

enum class EntityType {
    PARK,
    STATE,
    DISTRICT,
    CITY
}

data class PreviewTurbinePoint(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val statusLabel: String? = null,
)

data class EntityPreviewData(
    val id: String,
    val type: EntityType,
    val title: String,
    val subtitle: String,
    val statusLabel: String,
    val turbineCountLabel: String? = null,
    val capacityLabel: String? = null,
    val turbines: List<PreviewTurbinePoint> = emptyList(),
    val annualProductionGwh: Double?,
    val co2SavingsTons: Double?,
)

enum class PreviewSheetState {
    Hidden,
    Peek,
    Minimized,
}

@Composable
fun EntityPreviewSheet(
    modifier: Modifier = Modifier,
    previewData: EntityPreviewData,
    sheetState: PreviewSheetState,
    onOpenDetails: () -> Unit,
    onExpand: () -> Unit,
    onMinimize: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (sheetState == PreviewSheetState.Hidden) return

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
        label = "entityPreviewOffsetY",
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
                enabled = sheetState == PreviewSheetState.Minimized,
                onClick = onExpand,
            )
            .pointerInput(sheetState) {
                val minimizeThresholdPx = 86.dp.toPx()
                val expandThresholdPx = 44.dp.toPx()
                val dismissThresholdPx = 116.dp.toPx()
                val maxPeekPullPx = 82.dp.toPx()

                detectVerticalDragGestures(
                    onDragEnd = {
                        isDragging = false
                        when {
                            sheetState == PreviewSheetState.Peek &&
                                dragOffsetY > minimizeThresholdPx -> onMinimize()

                            sheetState == PreviewSheetState.Minimized &&
                                dragOffsetY < -expandThresholdPx -> onExpand()

                            sheetState == PreviewSheetState.Minimized &&
                                dragOffsetY > dismissThresholdPx -> onDismiss()
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
                            PreviewSheetState.Hidden -> 0f
                            PreviewSheetState.Peek -> {
                                val nextOffset = dragOffsetY + dragAmount
                                if (nextOffset < 0f) {
                                    (nextOffset * 0.28f).coerceAtLeast(-18.dp.toPx())
                                } else {
                                    (nextOffset * 0.9f).coerceAtMost(minimizeThresholdPx * 1.35f)
                                }
                            }

                            PreviewSheetState.Minimized -> {
                                val nextOffset = dragOffsetY + dragAmount
                                if (nextOffset < 0f) {
                                    (nextOffset * 0.62f).coerceAtLeast(-maxPeekPullPx)
                                } else {
                                    (nextOffset * 0.82f).coerceAtMost(dismissThresholdPx * 1.2f)
                                }
                            }
                        }
                    },
                )
            },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 16.dp,
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sheetState == PreviewSheetState.Minimized) {
                MinimizedContent(
                    previewData = previewData,
                    onDismiss = onDismiss,
                )
            } else {
                SheetHeader(
                    previewData = previewData,
                    onDismiss = onDismiss,
                )
                PeekContent(previewData = previewData)
                DetailsLink(onOpenDetails = onOpenDetails)
            }
        }
    }
}

@Composable
private fun SheetHeader(
    previewData: EntityPreviewData,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(WindklarTheme.colors.lightOverlayGreen),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Vorschau schließen",
                    tint = WindklarTheme.colors.mutedGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

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
                    text = previewData.title,
                    color = WindklarTheme.colors.darkGreen,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = WindklarTheme.colors.mutedGreen,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = previewData.subtitle,
                        color = WindklarTheme.colors.mutedGreen,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            StatusPill(text = previewData.statusLabel)
        }
    }
}

@Composable
private fun MinimizedContent(
    previewData: EntityPreviewData,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(WindklarTheme.colors.lightOverlayGreen),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Vorschau schließen",
                    tint = WindklarTheme.colors.mutedGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = previewData.title,
                    color = WindklarTheme.colors.darkGreen,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = previewData.subtitle,
                    color = WindklarTheme.colors.mutedGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(text = previewData.statusLabel)
            Surface(
                shape = CircleShape,
                color = WindklarTheme.colors.paleGreen,
            ) {
                Text(
                    text = "Öffnen",
                    color = WindklarTheme.colors.primaryGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PeekContent(previewData: EntityPreviewData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EntityVisualSummary(
            previewData = previewData,
            modifier = Modifier.height(112.dp),
        )
        PrimaryMetrics(previewData = previewData)
    }
}

@Composable
private fun DetailsLink(
    onOpenDetails: () -> Unit,
) {
    Surface(
        onClick = onOpenDetails,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = WindklarTheme.colors.paleGreen,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Alle Informationen öffnen",
                color = WindklarTheme.colors.primaryGreen,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = WindklarTheme.colors.primaryGreen,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun CompactEntityVisualSummary(
    title: String,
    statusLabel: String,
    primaryFact: String,
    secondaryFact: String?,
    isFavorite: Boolean? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(WindklarTheme.colors.paleGreen),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color(0xFF9BC79F).copy(alpha = 0.34f)
            val pointColor = Color(0xFF2D5A2D)
            drawLine(lineColor, Offset(size.width * 0.2f, size.height * 0.28f), Offset(size.width * 0.78f, size.height * 0.62f), strokeWidth = 2f)
            drawLine(lineColor, Offset(size.width * 0.24f, size.height * 0.72f), Offset(size.width * 0.72f, size.height * 0.22f), strokeWidth = 2f)
            listOf(
                Offset(size.width * 0.26f, size.height * 0.36f),
                Offset(size.width * 0.46f, size.height * 0.64f),
                Offset(size.width * 0.72f, size.height * 0.42f),
            ).forEach { center ->
                drawCircle(color = pointColor, radius = 4.5f, center = center)
                drawCircle(color = pointColor.copy(alpha = 0.14f), radius = 13f, center = center)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = WindklarTheme.colors.darkGreen,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(primaryFact, secondaryFact).joinToString(" · "),
                color = WindklarTheme.colors.primaryGreen,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        StatusPill(
            text = statusLabel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )

        if (isFavorite != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f),
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) WindklarTheme.colors.heartRed else WindklarTheme.colors.mutedGreen,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityVisualSummary(
    previewData: EntityPreviewData,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WindklarTheme.colors.paleGreen),
    ) {
        TurbineCanvas(
            points = previewData.turbines,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (previewData.type == EntityType.PARK) "Anlagen im Windpark" else "Region im Datensatz",
                color = WindklarTheme.colors.darkGreen,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                previewData.turbineCountLabel?.let { SummaryChip(text = it) }
                previewData.capacityLabel?.let { SummaryChip(text = it) }
            }
        }
    }
}

@Composable
private fun TurbineCanvas(
    points: List<PreviewTurbinePoint>,
    modifier: Modifier = Modifier,
) {
    val lineColor = WindklarTheme.colors.primaryGreen.copy(alpha = 0.18f)
    val gridColor = WindklarTheme.colors.primaryGreen.copy(alpha = 0.08f)
    val pointColor = WindklarTheme.colors.primaryGreen

    Canvas(modifier = modifier) {
        val padding = 24.dp.toPx()
        repeat(3) { index ->
            val ratio = (index + 1) / 4f
            drawLine(
                color = gridColor,
                start = Offset(padding, size.height * ratio),
                end = Offset(size.width - padding, size.height * ratio),
                strokeWidth = 1.2f,
            )
            drawLine(
                color = gridColor,
                start = Offset(size.width * ratio, padding),
                end = Offset(size.width * ratio, size.height - padding),
                strokeWidth = 1.2f,
            )
        }

        if (points.isEmpty()) return@Canvas

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latRange = (maxLat - minLat).takeIf { it > 0.000001 } ?: 1.0
        val lonRange = (maxLon - minLon).takeIf { it > 0.000001 } ?: 1.0
        val availableWidth = (size.width - padding * 2).coerceAtLeast(1f)
        val availableHeight = (size.height - padding * 2).coerceAtLeast(1f)
        val centers = points.map { point ->
            if (points.size == 1) {
                Offset(size.width * 0.5f, size.height * 0.56f)
            } else {
                val x = padding + (((point.longitude - minLon) / lonRange).toFloat() * availableWidth)
                val y = padding + ((1f - ((point.latitude - minLat) / latRange).toFloat()) * availableHeight)
                Offset(x, y)
            }
        }

        centers.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 2.5f,
            )
        }
        centers.forEach { center ->
            drawCircle(color = pointColor.copy(alpha = 0.14f), radius = 15f, center = center)
            drawCircle(color = pointColor, radius = 5.5f, center = center)
        }
    }
}

@Composable
private fun PrimaryMetrics(previewData: EntityPreviewData) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val prodStr = previewData.annualProductionGwh?.let {
            "${formatGermanNumber(it, 1)} GWh"
        } ?: "k.A."

        val co2Str = previewData.co2SavingsTons?.let {
            "${formatGermanNumber(it.toInt())} t"
        } ?: "k.A."

        MetricCard(
            modifier = Modifier.weight(1f),
            label = "Jahresproduktion",
            value = prodStr,
            icon = Icons.Outlined.Bolt,
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            label = "CO₂-Einsparung",
            value = co2Str,
            icon = Icons.Outlined.Eco,
        )
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
        shape = RoundedCornerShape(18.dp),
        color = WindklarTheme.colors.paleGreen,
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
                    tint = WindklarTheme.colors.primaryGreen,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    color = WindklarTheme.colors.primaryGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                color = WindklarTheme.colors.darkGreen,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.82f),
    ) {
        Text(
            text = text,
            color = WindklarTheme.colors.primaryGreen,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = WindklarTheme.colors.primaryGreen,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
