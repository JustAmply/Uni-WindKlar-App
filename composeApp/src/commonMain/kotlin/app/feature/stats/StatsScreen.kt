package app.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.ui.components.formatDataQuality
import app.core.ui.components.qualityColors

private val ScreenBackground = Color(0xFFF7FAF4)
private val PrimaryGreen = Color(0xFF24512D)
private val HeaderGreen = Color(0xFF2E7D32)
private val AccentGreen = Color(0xFF4CAF50)
private val DarkText = Color(0xFF17261A)
private val MutedText = Color(0xFF647568)
private val SoftGreen = Color(0xFFE8F5E9)
private val TrackGreen = Color(0xFFDDEBDD)
private val ImpactTones = listOf(
    ImpactTone(container = Color(0xFF24512D), content = Color.White, secondary = Color(0xFFEAF4E8)),
    ImpactTone(container = Color(0xFF3B7A3F), content = Color.White, secondary = Color(0xFFEAF4E8)),
    ImpactTone(container = Color(0xFFB8DDB8), content = DarkText, secondary = Color(0xFF36543B)),
    ImpactTone(container = Color(0xFFD6ECD2), content = DarkText, secondary = Color(0xFF4D6752)),
)

private data class ImpactTone(
    val container: Color,
    val content: Color,
    val secondary: Color,
)

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        StatsHeader(
            subtitle = uiState.subtitle,
            overviewCards = uiState.overviewCards,
        )

        Column(
            modifier = Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ImpactGrid(cards = uiState.impactCards)

            StatsSectionCard {
                SectionHeader(
                    title = "CO2-Einsparung einordnen",
                    subtitle = "Jahreswert mit groben Alltagsvergleichen",
                )
                Spacer(modifier = Modifier.height(14.dp))
                Co2ContextBlock(
                    summary = uiState.co2Summary,
                    values = uiState.co2Comparisons,
                )
                SourceFootnote(text = "Vergleichswerte sind gerundete Einordnungen auf Basis dokumentierter Annahmen.")
            }

            StatsSectionCard {
                SectionHeader(
                    title = "Kreise und kreisfreie Städte",
                    subtitle = "Top 5 nach installierter Leistung",
                )
                Spacer(modifier = Modifier.height(18.dp))
                DistrictBarChart(values = uiState.topDistricts)
                SourceFootnote(text = "Die Kreisebene wird im MVP aus den ersten fünf Stellen der AGS-Gemeindekennung abgeleitet; echte Kreisnamen sind noch nicht Teil des Snapshots.")
            }

            uiState.districtComparison?.let { comparison ->
                StatsSectionCard {
                    SectionHeader(
                        title = "Kreis des letzten Parks",
                        subtitle = if (comparison.isFallback) {
                            "Kein zuletzt geöffneter Park, daher stärkster Kreis im Snapshot"
                        } else {
                            "Aus dem zuletzt geöffneten Windpark abgeleitet"
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DistrictComparisonBlock(comparison = comparison)
                }
            }

            StatsSectionCard {
                SectionHeader(
                    title = "Leistungsklassen",
                    subtitle = "Windparks nach installierter Gesamtleistung",
                )
                Spacer(modifier = Modifier.height(16.dp))
                CapacityClassChart(values = uiState.capacityClasses)
            }

            StatsSectionCard {
                SectionHeader(
                    title = "Datenqualität",
                    subtitle = "Warum manche Werte Schätzungen sind",
                )
                Spacer(modifier = Modifier.height(12.dp))
                QualityNotes(notes = uiState.qualityNotes)
                if (uiState.attribution.isNotBlank()) {
                    SourceFootnote(text = uiState.attribution)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatsHeader(
    subtitle: String,
    overviewCards: List<StatsOverviewCard>,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(305.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryGreen, HeaderGreen),
                    start = Offset.Zero,
                    end = Offset(900f, 900f),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = (-64).dp)
                .size(224.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-24).dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        text = "Statistiken",
                        color = Color.White,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                overviewCards.take(3).forEach { card ->
                    OverviewCard(
                        card = card,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    card: StatsOverviewCard,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.18f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = card.icon.imageVector(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = card.value,
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.label,
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ImpactGrid(cards: List<StatsImpactCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.take(4).chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowCards.forEachIndexed { columnIndex, card ->
                    val toneIndex = cards.indexOf(card).takeIf { it >= 0 } ?: columnIndex
                    ImpactCard(
                        card = card,
                        tone = ImpactTones[toneIndex.coerceIn(0, ImpactTones.lastIndex)],
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowCards.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ImpactCard(
    card: StatsImpactCard,
    tone: ImpactTone,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 228.dp),
        shape = RoundedCornerShape(12.dp),
        color = tone.container,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(tone.content.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = card.icon.imageVector(),
                        contentDescription = null,
                        tint = tone.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = card.title,
                    color = tone.content,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = card.value,
                color = tone.content,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = card.description,
                color = tone.secondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.weight(1f))
            QualityPill(quality = card.quality)
        }
    }
}

@Composable
private fun Co2ContextBlock(
    summary: String,
    values: List<Co2Comparison>,
) {
    if (summary.isBlank() && values.isEmpty()) {
        EmptyText(text = "Noch keine CO2-Vergleichswerte verfügbar.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SoftGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.ifBlank { "Keine Angabe" },
                    color = DarkText,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "geschätzte vermiedene Emissionen pro Jahr",
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        values.forEach { value ->
            Co2ComparisonRow(value = value)
        }
    }
}

@Composable
private fun Co2ComparisonRow(value: Co2Comparison) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(PrimaryGreen),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = value.label,
                    color = DarkText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value.value,
                    color = DarkText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                text = value.description,
                color = MutedText,
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
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = DarkText,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            color = MutedText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun DistrictBarChart(values: List<DistrictStat>) {
    if (values.isEmpty()) {
        EmptyText(text = "Noch keine Kreiswerte verfügbar.")
        return
    }

    val maxValue = values.maxOf { it.installedCapacityMw }.toFloat().coerceAtLeast(1f)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.forEachIndexed { index, district ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${index + 1}",
                    color = PrimaryGreen,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = district.label,
                                color = DarkText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Schwerpunkt: ${district.contextLabel}",
                                color = MutedText,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "${district.installedCapacityMw.roundLabel()} MW",
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    ProgressTrack(progress = (district.installedCapacityMw.toFloat() / maxValue).coerceIn(0f, 1f))
                }
            }
        }
    }
}

@Composable
private fun DistrictComparisonBlock(comparison: DistrictComparison) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comparison.label,
                    color = DarkText,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = comparison.contextLabel,
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                Text(
                    text = comparison.rankText,
                    color = PrimaryGreen,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = comparison.nationalShare,
                color = DarkText,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        ProgressTrack(progress = comparison.shareProgress)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ComparisonValue(
                label = "Leistung",
                value = comparison.installedCapacity,
                modifier = Modifier.weight(1f),
            )
            ComparisonValue(
                label = "Windparks",
                value = comparison.windParks,
                modifier = Modifier.weight(1f),
            )
            ComparisonValue(
                label = "Anlagen",
                value = comparison.turbines,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ComparisonValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = ScreenBackground,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value,
                color = DarkText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun CapacityClassChart(values: List<CapacityClassStat>) {
    if (values.isEmpty()) {
        EmptyText(text = "Noch keine Leistungsklassen verfügbar.")
        return
    }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        ) {
            val slotWidth = size.width / values.size.coerceAtLeast(1)
            val barWidth = slotWidth * 0.58f
            values.forEachIndexed { index, value ->
                val height = value.share.coerceIn(0f, 1f) * size.height
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = if (index == values.lastIndex) AccentGreen else PrimaryGreen.copy(alpha = 0.72f),
                    topLeft = Offset(left, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                )
                drawRoundRect(
                    color = TrackGreen,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            values.forEach { value ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = value.label,
                        color = MutedText,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = value.count.toString(),
                        color = DarkText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityNotes(notes: List<StatsQualityNote>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        notes.forEach { note ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                QualityPill(quality = note.quality)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.label,
                        color = DarkText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = note.description,
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityPill(quality: String) {
    val colors = qualityColors(quality)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colors.container,
    ) {
        Text(
            text = formatDataQuality(quality),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = colors.content,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(TrackGreen),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(PrimaryGreen),
        )
    }
}

@Composable
private fun SourceFootnote(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 14.dp),
        color = MutedText,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        color = MutedText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

private fun StatsIcon.imageVector(): ImageVector = when (this) {
    StatsIcon.Wind -> Icons.Outlined.Air
    StatsIcon.Production -> Icons.Outlined.Bolt
    StatsIcon.Capacity -> Icons.Outlined.Bolt
    StatsIcon.Household -> Icons.Outlined.Home
    StatsIcon.Co2 -> Icons.Outlined.Eco
    StatsIcon.Money -> Icons.Outlined.MonetizationOn
    StatsIcon.District -> Icons.Outlined.LocationOn
    StatsIcon.DataQuality -> Icons.Outlined.Info
}

private fun Double.roundLabel(): String {
    val rounded = kotlin.math.round(this)
    return rounded.toInt().toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}
