package app.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.ui.theme.WindklarTheme
import org.jetbrains.compose.resources.painterResource
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.start_background
import kotlin.math.roundToInt
import app.core.util.formatGermanNumber

enum class EntityType {
    PARK,
    STATE,
    DISTRICT,
    CITY
}

data class EntityPreviewData(
    val id: String,
    val type: EntityType,
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val annualProductionGwh: Double?,
    val co2SavingsTons: Double?,
)

enum class PreviewSheetState {
    Expanded,
    Minimized,
}

@Composable
fun EntityPreviewSheet(
    modifier: Modifier = Modifier,
    previewData: EntityPreviewData,
    sheetState: PreviewSheetState,
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
        label = "entityPreviewOffsetY",
    )
    val sheetScaleY = 1f + ((-animatedOffsetY).coerceAtLeast(0f) / 700f)
    val sheetOffsetY = animatedOffsetY.coerceAtLeast(0f)

    LaunchedEffect(sheetState) {
        isDragging = false
        dragOffsetY = 0f
    }

    val LightOverlayGreen = Color(0xFFD8E7D8)

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
                val minimizeThresholdPx = 96.dp.toPx()
                val expandThresholdPx = 44.dp.toPx()
                val maxExpandedPullPx = 34.dp.toPx()
                val maxMinimizedPullPx = 82.dp.toPx()

                detectVerticalDragGestures(
                    onDragEnd = {
                        isDragging = false
                        when {
                            sheetState == PreviewSheetState.Expanded &&
                                dragOffsetY > minimizeThresholdPx -> onMinimize()

                            sheetState == PreviewSheetState.Minimized &&
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
                            PreviewSheetState.Expanded -> {
                                val nextOffset = dragOffsetY + dragAmount
                                if (nextOffset < 0f) {
                                    (nextOffset * 0.38f).coerceAtLeast(-maxExpandedPullPx)
                                } else {
                                    (nextOffset * 0.9f).coerceAtMost(minimizeThresholdPx * 1.35f)
                                }
                            }

                            PreviewSheetState.Minimized -> {
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
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(
                if (sheetState == PreviewSheetState.Expanded) 16.dp else 10.dp
            ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(LightOverlayGreen),
            )

            if (sheetState == PreviewSheetState.Minimized) {
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

                    Surface(
                        shape = CircleShape,
                        color = WindklarTheme.colors.paleGreen,
                    ) {
                        Text(
                            text = "Öffnen",
                            color = WindklarTheme.colors.primaryGreen,
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
                        contentDescription = "Vorschau Bild",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = CircleShape,
                        color = WindklarTheme.colors.primaryGreen,
                    ) {
                        Text(
                            text = previewData.badgeLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = previewData.title,
                        color = WindklarTheme.colors.darkGreen,
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
                            tint = WindklarTheme.colors.mutedGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = previewData.subtitle,
                            color = WindklarTheme.colors.mutedGreen,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

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
                        label = "CO2 Einsparung",
                        value = co2Str,
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
                        containerColor = WindklarTheme.colors.primaryGreen,
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
                )
            }
            Text(
                text = value,
                color = WindklarTheme.colors.darkGreen,
                fontSize = 18.sp,
                lineHeight = 28.sp,
            )
        }
    }
}


