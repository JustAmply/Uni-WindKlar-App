package app.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WindPower
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.core.model.RankingDetailLine
import app.core.ui.components.formatDataQuality
import app.core.ui.components.qualityColors
import app.core.ui.components.FullRankingDialog
import app.core.ui.components.TopRankingList
import app.core.ui.theme.WindklarTheme
import org.jetbrains.compose.resources.painterResource
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.header_background_quiet
import app.core.ui.components.WindklarHeader
import app.core.util.formatGermanNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenBackground @Composable get() = WindklarTheme.colors.screenBackground

private const val SearchSelectDebounceMillis = 180L
private const val SearchSelectVisibleOptionLimit = 50
private val PrimaryGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val HeaderGreen @Composable get() = WindklarTheme.colors.headerEndGreen
private val AccentGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val DarkText @Composable get() = WindklarTheme.colors.darkGreen
private val MutedText @Composable get() = WindklarTheme.colors.mutedGreen
private val SoftGreen @Composable get() = WindklarTheme.colors.paleGreen
private val ImpactTones @Composable get() = listOf(
    ImpactTone(container = Color.White, accent = PrimaryGreen, iconContainer = SoftGreen),
    ImpactTone(
        container = WindklarTheme.colors.impactToneContainer2,
        accent = HeaderGreen,
        iconContainer = WindklarTheme.colors.impactToneIcon2,
    ),
    ImpactTone(
        container = WindklarTheme.colors.impactToneContainer3,
        accent = WindklarTheme.colors.impactToneAccent3,
        iconContainer = WindklarTheme.colors.impactToneIcon3,
    ),
    ImpactTone(
        container = Color.White,
        accent = WindklarTheme.colors.mutedGreen,
        iconContainer = WindklarTheme.colors.impactToneIcon4,
    ),
)

private data class ImpactTone(
    val container: Color,
    val accent: Color,
    val iconContainer: Color,
)

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToParkDetail: (String) -> Unit,
    onNavigateToRegionDetail: (type: String, id: String) -> Unit,
    onNavigateToImpactDetail: (metricType: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()

    LaunchedEffect(viewModel) {
        viewModel.loadIfNeeded()
    }

    LaunchedEffect(uiState.selectedTab) {
        scrollState.scrollTo(0)
    }

    var showFullRankingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(scrollState),
    ) {
        StatsHeader(
            subtitle = uiState.subtitle,
            snapshotInfoLine = uiState.snapshotInfoLine,
            overviewCards = uiState.overviewCards,
        )

        Column(
            modifier = Modifier
                .offset(y = (-32).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatsTabSwitch(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::setSelectedTab,
            )

            when (uiState.selectedTab) {
                StatsTab.OVERVIEW -> {
                    ImpactGrid(
                        cards = uiState.impactCards,
                        onCardClick = { card -> onNavigateToImpactDetail(card.type.name) },
                    )

                    StatsSectionCard {
                        SectionHeader(
                            title = "Größenklassen der Windparks",
                            subtitle = "Installierte Gesamtleistung pro Windpark, nicht pro einzelner Windanlage",
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CapacityClassChart(values = uiState.capacityClasses)
                        SourceFootnote(text = "Die Klassen sind bewusst grob, damit die Verteilung lesbar bleibt.")
                    }

                    uiState.districtComparison?.let { comparison ->
                        StatsSectionCard(
                            modifier = Modifier.clickable {
                                onNavigateToRegionDetail("district", comparison.districtId)
                            },
                        ) {
                            SectionHeader(
                                title = "Landkreis zum letzten Windpark",
                                subtitle = "Aus dem zuletzt angesehenen Windpark abgeleitet",
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            DistrictComparisonBlock(comparison = comparison)
                        }
                    }

                    StatsSectionCard {
                        CollapsibleQualityNotes(notes = uiState.qualityNotes)
                        if (uiState.attribution.isNotBlank()) {
                            SourceFootnote(text = uiState.attribution)
                        }
                    }
                }
                StatsTab.RANKINGS -> {
                    StatsSectionCard {
                        SectionHeader(
                            title = "Top-Ranglisten",
                            subtitle = "Ranglisten nach installierter Leistung",
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        RankingTypeSwitch(
                            selectedType = uiState.rankingType,
                            onSelected = viewModel::setRankingType,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        TopRankingList(
                            values = uiState.rankingItems,
                            onActionClick = { itemId ->
                                viewModel.selectInComparison(uiState.rankingType, itemId)
                            },
                            onDetailsClick = { itemId ->
                                when (uiState.rankingType) {
                                    RankingType.PARKS -> onNavigateToParkDetail(itemId)
                                    RankingType.CITIES -> onNavigateToRegionDetail("city", itemId)
                                    RankingType.DISTRICTS -> onNavigateToRegionDetail("district", itemId)
                                    RankingType.STATES -> onNavigateToRegionDetail("state", itemId)
                                }
                            },
                            onShowFullListClick = { showFullRankingDialog = true },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SourceFootnote(
                            text = when (uiState.rankingType) {
                                RankingType.PARKS -> "Windparks im Snapshot basierend auf MaStR-Stammdaten."
                                RankingType.CITIES -> "Gemeinden aggregiert nach dem Gemeindeschlüssel (AGS)."
                                RankingType.DISTRICTS -> "Die Kreisebene wird aus den ersten fünf Stellen der AGS-Gemeindekennung abgeleitet; fehlende Kreisnamen werden angenähert."
                                RankingType.STATES -> "Bundesländer aggregiert auf Basis der offiziellen Länderkennungen."
                            }
                        )
                    }
                }
                StatsTab.COMPARISON -> {
                    InteractiveComparisonCard(
                        uiState = uiState,
                        onComparisonTypeChange = viewModel::setComparisonType,
                        onSelectParkA = viewModel::selectParkA,
                        onSelectParkB = viewModel::selectParkB,
                        onSelectCityA = viewModel::selectCityA,
                        onSelectCityB = viewModel::selectCityB,
                        onSelectDistrictA = viewModel::selectDistrictA,
                        onSelectDistrictB = viewModel::selectDistrictB,
                        onSelectStateA = viewModel::selectStateA,
                        onSelectStateB = viewModel::selectStateB,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (showFullRankingDialog) {
        FullRankingDialog(
            title = when (uiState.rankingType) {
                RankingType.PARKS -> "Rangliste: Windparks"
                RankingType.CITIES -> "Rangliste: Gemeinden"
                RankingType.DISTRICTS -> "Rangliste: Landkreise"
                RankingType.STATES -> "Rangliste: Bundesländer"
            },
            rankingItems = uiState.rankingItems,
            onDismiss = { showFullRankingDialog = false },
            onActionClick = { itemId ->
                viewModel.selectInComparison(uiState.rankingType, itemId)
                showFullRankingDialog = false
            },
            onDetailsClick = { itemId ->
                showFullRankingDialog = false
                when (uiState.rankingType) {
                    RankingType.PARKS -> onNavigateToParkDetail(itemId)
                    RankingType.CITIES -> onNavigateToRegionDetail("city", itemId)
                    RankingType.DISTRICTS -> onNavigateToRegionDetail("district", itemId)
                    RankingType.STATES -> onNavigateToRegionDetail("state", itemId)
                }
            }
        )
    }
}

@Composable
private fun StatsTabSwitch(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(StatsTab.OVERVIEW, StatsTab.RANKINGS, StatsTab.COMPARISON).forEach { tab ->
                val selected = tab == selectedTab
                val label = when (tab) {
                    StatsTab.OVERVIEW -> "Überblick"
                    StatsTab.RANKINGS -> "Ranglisten"
                    StatsTab.COMPARISON -> "Vergleich"
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) PrimaryGreen else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(
    subtitle: String,
    snapshotInfoLine: String,
    overviewCards: List<StatsOverviewCard>,
) {
    WindklarHeader(
        title = "Statistik",
        subtitle = subtitle,
        snapshotInfoLine = snapshotInfoLine,
        showDecorativeCircles = false,
        backgroundPainter = painterResource(Res.drawable.header_background_quiet),
        bottomPadding = 48.dp,
        extraContent = {
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
    )
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
private fun ImpactGrid(
    cards: List<StatsImpactCard>,
    onCardClick: (StatsImpactCard) -> Unit,
) {
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
                        modifier = Modifier
                            .weight(1f)
                            .height(220.dp),
                        onClick = { onCardClick(card) },
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
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = tone.container,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tone.iconContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = card.icon.imageVector(),
                        contentDescription = null,
                        tint = tone.accent,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Text(
                    text = card.title,
                    color = DarkText,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = card.value,
                color = DarkText,
                fontSize = 25.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = card.description,
                color = MutedText,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.metaLabel,
                    color = MutedText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Details",
                        color = tone.accent,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = tone.accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WindklarTheme.colors.cardBackground,
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
private fun RankingTypeSwitch(
    selectedType: RankingType,
    onSelected: (RankingType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ScreenBackground)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        RankingTypeSegment(
            label = "Windparks",
            selected = selectedType == RankingType.PARKS,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(RankingType.PARKS) },
        )
        RankingTypeSegment(
            label = "Gemeinden",
            selected = selectedType == RankingType.CITIES,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(RankingType.CITIES) },
        )
        RankingTypeSegment(
            label = "Landkreise",
            selected = selectedType == RankingType.DISTRICTS,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(RankingType.DISTRICTS) },
        )
        RankingTypeSegment(
            label = "Länder",
            selected = selectedType == RankingType.STATES,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(RankingType.STATES) },
        )
    }
}

@Composable
private fun RankingTypeSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) PrimaryGreen else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Color.White else MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                label = "Windanlagen",
                value = comparison.turbines,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SoftGreen)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Kreisdetails öffnen",
                color = PrimaryGreen,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(18.dp),
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

    val primaryGreen = PrimaryGreen
    val accentGreen = AccentGreen
    val trackGreen = WindklarTheme.colors.trackGreen

    var selectedIndex by remember(values) { mutableStateOf<Int?>(null) }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(96.dp)
                    .pointerInput(values) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / values.size.coerceAtLeast(1)
                            val tappedIndex = (offset.x / slotWidth).toInt()
                            selectedIndex = if (tappedIndex in values.indices) {
                                tappedIndex.takeUnless { it == selectedIndex }
                            } else {
                                null
                            }
                        }
                    },
            ) {
                val slotWidth = size.width / values.size.coerceAtLeast(1)
                val barWidth = slotWidth * 0.58f
                val cornerRadiusPx = 7.dp.toPx()
                val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                val stroke = Stroke(width = 1.dp.toPx())

                values.forEachIndexed { index, value ->
                    val height = value.share.coerceIn(0f, 1f) * size.height
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val isSelected = selectedIndex == null || selectedIndex == index
                    drawRoundRect(
                        color = if (index == values.lastIndex) {
                            accentGreen.copy(alpha = if (isSelected) 1f else 0.34f)
                        } else {
                            primaryGreen.copy(alpha = if (isSelected) 0.86f else 0.30f)
                        },
                        topLeft = Offset(left, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = cornerRadius,
                    )
                    drawRoundRect(
                        color = trackGreen,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = cornerRadius,
                        style = stroke,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                values.forEachIndexed { index, value ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (selectedIndex == index) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkText,
                                shadowElevation = 3.dp,
                            ) {
                                Text(
                                    text = "${formatGermanNumber(value.count)} Windparks (${value.percentOfTotal.percentLabel()})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
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
                        text = value.description,
                        color = MutedText.copy(alpha = 0.78f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatGermanNumber(value.count),
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
private fun InteractiveComparisonCard(
    uiState: StatsUiState,
    onComparisonTypeChange: (ComparisonType) -> Unit,
    onSelectParkA: (String?) -> Unit,
    onSelectParkB: (String?) -> Unit,
    onSelectCityA: (String?) -> Unit,
    onSelectCityB: (String?) -> Unit,
    onSelectDistrictA: (String?) -> Unit,
    onSelectDistrictB: (String?) -> Unit,
    onSelectStateA: (String?) -> Unit,
    onSelectStateB: (String?) -> Unit,
) {
    var dialogTarget by remember { mutableStateOf<ComparisonDialogTarget?>(null) }

    StatsSectionCard {
        SectionHeader(
            title = "Direktvergleich",
            subtitle = "Zwei Gemeinden, Landkreise, Bundesländer oder Windparks gegenüberstellen",
        )
        Spacer(modifier = Modifier.height(14.dp))
        ComparisonTypeSwitch(
            selectedType = uiState.comparisonType,
            onSelected = onComparisonTypeChange,
        )
        Spacer(modifier = Modifier.height(14.dp))

        val optionA = when (uiState.comparisonType) {
            ComparisonType.PARKS -> uiState.selectedParkA
            ComparisonType.CITIES -> uiState.selectedCityA
            ComparisonType.DISTRICTS -> uiState.selectedDistrictA
            ComparisonType.STATES -> uiState.selectedStateA
        }
        val optionB = when (uiState.comparisonType) {
            ComparisonType.PARKS -> uiState.selectedParkB
            ComparisonType.CITIES -> uiState.selectedCityB
            ComparisonType.DISTRICTS -> uiState.selectedDistrictB
            ComparisonType.STATES -> uiState.selectedStateB
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ComparisonSlotButton(
                slotLabel = "A",
                option = optionA,
                modifier = Modifier.weight(1f),
                onClick = {
                    dialogTarget = when (uiState.comparisonType) {
                        ComparisonType.PARKS -> ComparisonDialogTarget.ParkA
                        ComparisonType.CITIES -> ComparisonDialogTarget.CityA
                        ComparisonType.DISTRICTS -> ComparisonDialogTarget.DistrictA
                        ComparisonType.STATES -> ComparisonDialogTarget.StateA
                    }
                },
            )
            ComparisonSlotButton(
                slotLabel = "B",
                option = optionB,
                modifier = Modifier.weight(1f),
                onClick = {
                    dialogTarget = when (uiState.comparisonType) {
                        ComparisonType.PARKS -> ComparisonDialogTarget.ParkB
                        ComparisonType.CITIES -> ComparisonDialogTarget.CityB
                        ComparisonType.DISTRICTS -> ComparisonDialogTarget.DistrictB
                        ComparisonType.STATES -> ComparisonDialogTarget.StateB
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        if (uiState.comparisonRows.isEmpty()) {
            EmptyText(text = "Noch keine Vergleichswerte verfügbar.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                uiState.comparisonRows.forEach { row ->
                    ComparisonMetricRow(row = row)
                }
            }
        }
    }

    dialogTarget?.let { target ->
        val options = when (target) {
            ComparisonDialogTarget.ParkA,
            ComparisonDialogTarget.ParkB -> uiState.allParks
            ComparisonDialogTarget.CityA,
            ComparisonDialogTarget.CityB -> uiState.allCities
            ComparisonDialogTarget.DistrictA,
            ComparisonDialogTarget.DistrictB -> uiState.allDistricts
            ComparisonDialogTarget.StateA,
            ComparisonDialogTarget.StateB -> uiState.allStates
        }
        SearchSelectDialog(
            title = when (target) {
                ComparisonDialogTarget.ParkA,
                ComparisonDialogTarget.ParkB -> "Windpark auswählen"
                ComparisonDialogTarget.CityA,
                ComparisonDialogTarget.CityB -> "Gemeinde auswählen"
                ComparisonDialogTarget.DistrictA,
                ComparisonDialogTarget.DistrictB -> "Landkreis auswählen"
                ComparisonDialogTarget.StateA,
                ComparisonDialogTarget.StateB -> "Bundesland auswählen"
            },
            options = options,
            onDismiss = { dialogTarget = null },
            onSelected = { option ->
                when (target) {
                    ComparisonDialogTarget.ParkA -> onSelectParkA(option.id)
                    ComparisonDialogTarget.ParkB -> onSelectParkB(option.id)
                    ComparisonDialogTarget.CityA -> onSelectCityA(option.id)
                    ComparisonDialogTarget.CityB -> onSelectCityB(option.id)
                    ComparisonDialogTarget.DistrictA -> onSelectDistrictA(option.id)
                    ComparisonDialogTarget.DistrictB -> onSelectDistrictB(option.id)
                    ComparisonDialogTarget.StateA -> onSelectStateA(option.id)
                    ComparisonDialogTarget.StateB -> onSelectStateB(option.id)
                }
                dialogTarget = null
            },
        )
    }
}

@Composable
private fun ComparisonTypeSwitch(
    selectedType: ComparisonType,
    onSelected: (ComparisonType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ScreenBackground)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ComparisonTypeSegment(
            label = "Windparks",
            selected = selectedType == ComparisonType.PARKS,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ComparisonType.PARKS) },
        )
        ComparisonTypeSegment(
            label = "Gemeinden",
            selected = selectedType == ComparisonType.CITIES,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ComparisonType.CITIES) },
        )
        ComparisonTypeSegment(
            label = "Landkreise",
            selected = selectedType == ComparisonType.DISTRICTS,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ComparisonType.DISTRICTS) },
        )
        ComparisonTypeSegment(
            label = "Länder",
            selected = selectedType == ComparisonType.STATES,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ComparisonType.STATES) },
        )
    }
}

@Composable
private fun ComparisonTypeSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) PrimaryGreen else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Color.White else MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ComparisonSlotButton(
    slotLabel: String,
    option: ComparisonOption?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = ScreenBackground,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Slot $slotLabel",
                    color = PrimaryGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = option?.label ?: "Auswählen",
                color = DarkText,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option?.description ?: "Noch kein Eintrag",
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ComparisonMetricRow(row: ComparisonRow) {
    val leftColor = if (row.isHigherA) PrimaryGreen else MutedText.copy(alpha = 0.45f)
    val rightColor = if (!row.isHigherA) PrimaryGreen else MutedText.copy(alpha = 0.45f)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = row.valueA,
                color = DarkText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = row.label,
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.25f),
            )
            Text(
                text = row.valueB,
                color = DarkText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp))
                    .background(WindklarTheme.colors.trackGreen),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(row.ratioA.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp))
                        .background(leftColor),
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp))
                    .background(WindklarTheme.colors.trackGreen),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(row.ratioB.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp))
                        .background(rightColor),
                )
            }
        }
    }
}

@Composable
private fun SearchSelectDialog(
    title: String,
    options: List<ComparisonOption>,
    onDismiss: () -> Unit,
    onSelected: (ComparisonOption) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(SearchSelectDebounceMillis)
        debouncedQuery = query
    }

    val filteredOptions = remember(debouncedQuery, options) {
        val normalizedQuery = debouncedQuery.trim()
        val matches = if (normalizedQuery.isBlank()) {
            options.asSequence()
        } else {
            options.asSequence().filter { option ->
                option.label.contains(normalizedQuery, ignoreCase = true) ||
                    option.description.contains(normalizedQuery, ignoreCase = true)
            }
        }
        matches.take(SearchSelectVisibleOptionLimit + 1).toList()
    }
    val visibleOptions = filteredOptions.take(SearchSelectVisibleOptionLimit)
    val hasMoreOptions = filteredOptions.size > SearchSelectVisibleOptionLimit

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(12.dp),
            color = WindklarTheme.colors.cardBackground,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        color = DarkText,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onDismiss) {
                        Text(text = "Schließen")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MutedText,
                        )
                    },
                    label = { Text("Suchen") },
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(visibleOptions, key = { it.id }) { option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelected(option) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = option.label,
                                color = DarkText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = option.description,
                                color = MutedText,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (hasMoreOptions) {
                        item {
                            Text(
                                text = "Mehr Treffer verfügbar. Bitte Suche verfeinern.",
                                color = MutedText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class ComparisonDialogTarget {
    ParkA,
    ParkB,
    CityA,
    CityB,
    DistrictA,
    DistrictB,
    StateA,
    StateB,
}

@Composable
private fun CollapsibleQualityNotes(notes: List<StatsQualityNote>) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(SoftGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Datenqualität",
                    color = DarkText,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Stammdaten offiziell, Windparks abgeleitet, Wirkungswerte geschätzt.",
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                tint = PrimaryGreen,
                modifier = Modifier.size(22.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            QualityNotes(notes = notes)
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
            .background(WindklarTheme.colors.trackGreen),
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
    StatsIcon.Wind -> Icons.Outlined.WindPower
    StatsIcon.Production -> Icons.Outlined.Bolt
    StatsIcon.Capacity -> Icons.Outlined.Bolt
    StatsIcon.Household -> Icons.Outlined.Home
    StatsIcon.Co2 -> Icons.Outlined.Eco
    StatsIcon.Money -> Icons.Outlined.Euro
    StatsIcon.District -> Icons.Outlined.LocationOn
    StatsIcon.DataQuality -> Icons.Outlined.Info
}

private fun Double.roundLabel(): String = formatGermanNumber(kotlin.math.round(this).toInt())

private fun Double.roundOneDecimal(): String = formatGermanNumber(this, 1)

private fun Float.percentLabel(): String = "${formatGermanNumber(this * 100.0f, 1)} %"
