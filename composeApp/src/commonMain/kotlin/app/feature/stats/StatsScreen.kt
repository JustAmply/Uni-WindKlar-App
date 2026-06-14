package app.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBackground = Color(0xFFF8FAF7)
private val PrimaryGreen = Color(0xFF2D5A2D)
private val HeaderEndGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF1A3A1A)
private val MutedGreen = Color(0xFF5A7A5A)
private val PaleGreen = Color(0xFFE8F5E9)
private val ChartGridGreen = Color(0xFFD8ECD9)

@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: StatsUiState = StatsUiState(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        StatsHeader(
            metrics = uiState.metrics,
            onBackClick = onBackClick,
        )

        Column(
            modifier = Modifier
                .offset(y = (-16).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatsSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(text = "Jaehrliches Wachstum")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = HeaderEndGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = uiState.growthPercentage,
                            color = HeaderEndGreen,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnnualGrowthChart(points = uiState.annualProduction)

                Text(
                    text = "Energieproduktion in TWh",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    color = MutedGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }

            StatsSectionCard {
                SectionTitle(text = "CO2 Reduktion")

                Spacer(modifier = Modifier.height(16.dp))

                Co2ReductionCard(co2Reduction = uiState.co2Reduction)
            }

            StatsSectionCard {
                SectionTitle(text = "Erneuerbare Energien Mix")

                Spacer(modifier = Modifier.height(16.dp))

                EnergyMixBarChart(values = uiState.energyMix)

                Text(
                    text = "Anteil in Prozent",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    color = MutedGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatsHeader(
    metrics: List<StatsMetric>,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryGreen, HeaderEndGreen),
                    start = Offset.Zero,
                    end = Offset(900f, 900f),
                ),
            )
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurueck",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = "Statistiken",
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            metrics.forEach { metric ->
                StatsMetricCard(
                    metric = metric,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatsMetricCard(
    metric: StatsMetric,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.2f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = metric.icon.imageVector(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = metric.value,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = metric.label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun StatsSectionCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = DarkGreen,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun AnnualGrowthChart(points: List<AnnualProductionPoint>) {
    val yLabels = listOf(800, 600, 400, 200, 0)
    val maxValue = 800f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
        ) {
            ChartYAxis(labels = yLabels)

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                val gridStroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)),
                )

                yLabels.forEachIndexed { index, _ ->
                    val y = size.height * index / (yLabels.size - 1)
                    drawLine(
                        color = ChartGridGreen,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridStroke.pathEffect,
                    )
                }

                points.forEachIndexed { index, _ ->
                    val x = size.width * index / (points.lastIndex.coerceAtLeast(1))
                    drawLine(
                        color = ChartGridGreen,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridStroke.pathEffect,
                    )
                }

                drawLine(
                    color = MutedGreen,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )

                if (points.isNotEmpty()) {
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = size.width * index / (points.lastIndex.coerceAtLeast(1))
                        val y = size.height - (point.value / maxValue).coerceIn(0f, 1f) * size.height
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = PrimaryGreen,
                        style = Stroke(width = 3.dp.toPx()),
                    )

                    points.forEachIndexed { index, point ->
                        val x = size.width * index / (points.lastIndex.coerceAtLeast(1))
                        val y = size.height - (point.value / maxValue).coerceIn(0f, 1f) * size.height
                        drawCircle(
                            color = PrimaryGreen,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEach { point ->
                Text(
                    text = point.year,
                    color = MutedGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun EnergyMixBarChart(values: List<EnergyMixValue>) {
    val yLabels = listOf(60, 45, 30, 15, 0)
    val maxValue = 60f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
        ) {
            ChartYAxis(labels = yLabels)

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                val gridStroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)),
                )

                yLabels.forEachIndexed { index, _ ->
                    val y = size.height * index / (yLabels.size - 1)
                    drawLine(
                        color = ChartGridGreen,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridStroke.pathEffect,
                    )
                }

                values.forEachIndexed { index, _ ->
                    val x = size.width * (index + 0.5f) / values.size.coerceAtLeast(1)
                    drawLine(
                        color = ChartGridGreen,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridStroke.pathEffect,
                    )
                }

                drawLine(
                    color = MutedGreen,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )

                val barColors = listOf(PrimaryGreen, Color(0xFF7CB342), Color(0xFFC8E6C9))
                val slotWidth = size.width / values.size.coerceAtLeast(1)
                val barWidth = slotWidth * 0.68f

                values.forEachIndexed { index, value ->
                    val barHeight = (value.percentage / maxValue).coerceIn(0f, 1f) * size.height
                    val left = slotWidth * index + (slotWidth - barWidth) / 2f
                    drawRoundRect(
                        color = barColors[index % barColors.size],
                        topLeft = Offset(left, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            values.forEach { value ->
                Text(
                    text = value.label,
                    color = MutedGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChartYAxis(labels: List<Int>) {
    Column(
        modifier = Modifier
            .width(34.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        labels.forEach { label ->
            Text(
                text = label.toString(),
                color = MutedGreen,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.End,
            )
        }
    }

    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
private fun Co2ReductionCard(co2Reduction: Co2Reduction) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PaleGreen, Color(0xFFC8E6C9)),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = co2Reduction.value,
                color = PrimaryGreen,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = co2Reduction.label,
                color = MutedGreen,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = co2Reduction.equivalent,
                modifier = Modifier.padding(top = 10.dp),
                color = MutedGreen,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun StatsMetricIcon.imageVector(): ImageVector = when (this) {
    StatsMetricIcon.Wind -> Icons.Outlined.Air
    StatsMetricIcon.Production -> Icons.Outlined.Bolt
    StatsMetricIcon.Renewable -> Icons.Outlined.Eco
}
