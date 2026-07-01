package app.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.WindPower
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.model.RankingDetailLine
import app.core.model.RankingItem
import app.core.ui.components.BreadcrumbSegment
import app.core.ui.components.FactList
import app.core.ui.components.FactListItem
import app.core.ui.components.FullRankingDialog
import app.core.ui.components.TopRankingList
import app.core.ui.components.WindklarHeader
import app.core.ui.theme.WindklarTheme
import org.jetbrains.compose.resources.painterResource
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.header_background_quiet

@Composable
fun ImpactDetailScreen(
    uiState: ImpactDetailUiState,
    onBack: () -> Unit,
    onNavigateToParkDetail: (String) -> Unit,
    onNavigateToRegionDetail: (String, String) -> Unit,
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WindklarTheme.colors.screenBackground),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = WindklarTheme.colors.primaryGreen)
        }
        return
    }

    val metricTitle = when (uiState.metricType) {
        "Households" -> "Haushalte"
        "MunicipalBenefit" -> "Kommunaler Nutzen"
        "Turbines" -> "Windanlagen"
        "Co2" -> "CO₂ gespart"
        else -> "Auswertung"
    }
    val metricIcon = uiState.metricType.impactDetailIcon()
    val metricSubtitle = when (uiState.metricType) {
        "Turbines" -> "Snapshot der Stammdaten"
        else -> "Deutschlandweite Einordnung"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WindklarTheme.colors.screenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        WindklarHeader(
            title = metricTitle,
            subtitle = metricSubtitle,
            showDecorativeCircles = false,
            backgroundPainter = painterResource(Res.drawable.header_background_quiet),
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            actionIcon = {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = metricIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.68f),
                        modifier = Modifier.size(30.dp),
                    )
                }
            },
            breadcrumbs = {
                BreadcrumbSegment(name = "Statistik")
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-32).dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState.metricType) {
                "Households" -> uiState.householdsDetail?.let { HouseholdsDetailContent(it, onNavigateToParkDetail, onNavigateToRegionDetail) }
                "MunicipalBenefit" -> uiState.municipalBenefitDetail?.let { MunicipalBenefitDetailContent(it, onNavigateToRegionDetail) }
                "Turbines" -> uiState.turbinesDetail?.let { TurbinesDetailContent(it, onNavigateToParkDetail, onNavigateToRegionDetail) }
                "Co2" -> uiState.co2Detail?.let { Co2DetailContent(it, onNavigateToParkDetail, onNavigateToRegionDetail) }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private val PrimaryGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val AccentGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val DarkText @Composable get() = WindklarTheme.colors.darkGreen
private val MutedText @Composable get() = WindklarTheme.colors.mutedGreen
private val SoftGreen @Composable get() = WindklarTheme.colors.paleGreen

@Composable
private fun ImpactSectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = DarkText,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ImpactSummaryBlock(
    label: String,
    value: String,
    subtitle: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = DarkText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                color = PrimaryGreen,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun ImpactRankingList(
    entries: List<ImpactBarEntry>,
    fullListTitle: String,
    onNavigateToParkDetail: (String) -> Unit,
    onNavigateToRegionDetail: (String, String) -> Unit,
) {
    var showFullRankingDialog by remember { mutableStateOf(false) }
    val items = entries.mapIndexed { index, entry ->
        ImpactRankingEntry(
            item = RankingItem(
                id = entry.navigateTarget?.rankingId() ?: "impact-${index + 1}",
                rank = index + 1,
                name = entry.label,
                subtitle = entry.navigateTarget.subtitleLabel(),
                valueLabel = entry.value,
                progress = entry.ratio,
                details = listOf(
                    RankingDetailLine("Wert", entry.value),
                    RankingDetailLine("Einordnung", entry.navigateTarget.subtitleLabel()),
                ),
            ),
            target = entry.navigateTarget,
        )
    }

    TopRankingList(
        values = items.map { it.item },
        onShowFullListClick = { showFullRankingDialog = true },
        onDetailsClick = { id ->
            items.firstOrNull { it.item.id == id }?.target?.let { target ->
                when (target) {
                    is ImpactNavigateTarget.Park -> onNavigateToParkDetail(target.id)
                    is ImpactNavigateTarget.Region -> onNavigateToRegionDetail(target.type, target.id)
                }
            }
        },
    )

    if (showFullRankingDialog) {
        FullRankingDialog(
            title = fullListTitle,
            rankingItems = items.map { it.item },
            onDismiss = { showFullRankingDialog = false },
            onDetailsClick = { id ->
                showFullRankingDialog = false
                items.firstOrNull { it.item.id == id }?.target?.let { target ->
                    when (target) {
                        is ImpactNavigateTarget.Park -> onNavigateToParkDetail(target.id)
                        is ImpactNavigateTarget.Region -> onNavigateToRegionDetail(target.type, target.id)
                    }
                }
            },
        )
    }
}

@Composable
private fun ImpactAssumptionsFooter(
    assumptions: List<StatsImpactFact>,
    qualityLabel: String,
) {
    FactList(
        items = assumptions.toFactListItems() + FactListItem("Datenqualität", qualityLabel),
    )
}

@Composable
private fun HistogramChart(entries: List<ImpactBarEntry>, accentLast: Boolean = false) {
    if (entries.isEmpty()) {
        Text(text = "Keine Daten verfügbar.", color = MutedText, fontSize = 13.sp, lineHeight = 18.sp)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val primaryGreen = PrimaryGreen
        val accentGreen = AccentGreen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                val slotWidth = size.width / entries.size.coerceAtLeast(1)
                val barWidth = slotWidth * 0.58f
                val cornerRadiusPx = 6.dp.toPx()
                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)

                entries.forEachIndexed { index, value ->
                    val height = value.ratio.coerceIn(0f, 1f) * size.height
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val isLast = accentLast && index == entries.lastIndex
                    drawRoundRect(
                        color = if (isLast) accentGreen else primaryGreen.copy(alpha = 0.86f),
                        topLeft = Offset(left, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.forEach { value ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = value.value,
                        color = DarkText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = value.label,
                        color = MutedText,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HouseholdsDetailContent(
    detail: HouseholdsImpactDetail,
    onNavigateToParkDetail: (String) -> Unit,
    onNavigateToRegionDetail: (String, String) -> Unit,
) {
    ImpactSummaryBlock(
        label = "Rechnerisch versorgbar",
        value = detail.summaryValue,
        subtitle = detail.summarySubtitle,
    )
    ImpactSectionCard {
        SectionTitle(title = "Was bedeutet das?")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Die Zahl setzt die geschätzte Jahresproduktion ins Verhältnis zu 3.500 kWh pro Haushalt und Jahr. Sie beschreibt eine rechnerische Größenordnung, keine direkte Belieferung.",
            color = DarkText,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        FactList(
            items = listOf(
                FactListItem("Anteil aller deutschen Haushalte", detail.nationalSharePercent),
                FactListItem("Durchschnitt pro Windpark", detail.avgPerPark),
            )
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Top-Windparks nach versorgten Haushalten")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactRankingList(
            entries = detail.topParks,
            fullListTitle = "Rangliste: Windparks nach versorgten Haushalten",
            onNavigateToParkDetail = onNavigateToParkDetail,
            onNavigateToRegionDetail = onNavigateToRegionDetail,
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Annahmen")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactAssumptionsFooter(assumptions = detail.assumptions, qualityLabel = detail.qualityLabel)
    }
}

@Composable
private fun MunicipalBenefitDetailContent(
    detail: MunicipalBenefitImpactDetail,
    onNavigateToRegionDetail: (String, String) -> Unit,
) {
    ImpactSummaryBlock(
        label = "Möglicher Betrag",
        value = detail.summaryValue,
        subtitle = detail.summarySubtitle,
    )
    ImpactSectionCard {
        SectionTitle(title = "Was bedeutet das?")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Der Betrag schätzt, was Gemeinden rechnerisch erhalten könnten. Ob tatsächlich gezahlt wird, hängt von lokalen Vereinbarungen ab.",
            color = DarkText,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        SectionTitle(title = "Spannweite pro Windpark")
        Spacer(modifier = Modifier.height(10.dp))
        FactList(
            items = listOf(
                FactListItem("Minimum", detail.minPerPark),
                FactListItem("Median", detail.medianPerPark),
                FactListItem("Maximum", detail.maxPerPark),
            )
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Top-Landkreise nach möglichem Nutzen")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactRankingList(
            entries = detail.topDistricts,
            fullListTitle = "Rangliste: Landkreise nach möglichem Nutzen",
            onNavigateToParkDetail = {},
            onNavigateToRegionDetail = onNavigateToRegionDetail,
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Annahmen")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactAssumptionsFooter(assumptions = detail.assumptions, qualityLabel = detail.qualityLabel)
    }
}

@Composable
private fun TurbinesDetailContent(
    detail: TurbinesImpactDetail,
    onNavigateToParkDetail: (String) -> Unit,
    onNavigateToRegionDetail: (String, String) -> Unit,
) {
    ImpactSummaryBlock(
        label = "Erfasste Windanlagen",
        value = detail.summaryValue,
        subtitle = detail.summarySubtitle,
    )
    ImpactSectionCard {
        SectionTitle(title = "Was bedeutet das?")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Gezählt werden einzelne Windanlagen, nicht Windparks. Windparks sind in WindKlar die bürgernahe Gruppierung mehrerer Windanlagen.",
            color = DarkText,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        FactList(items = listOf(FactListItem("Durchschnitt pro Windpark", detail.avgPerPark)))
    }
    ImpactSectionCard {
        SectionTitle(title = "Inbetriebnahme nach Jahrzehnt")
        Spacer(modifier = Modifier.height(14.dp))
        HistogramChart(entries = detail.byDecade, accentLast = true)
    }
    ImpactSectionCard {
        SectionTitle(title = "Nabenhöhe")
        Spacer(modifier = Modifier.height(14.dp))
        HistogramChart(entries = detail.heightBuckets)
    }
    ImpactSectionCard {
        SectionTitle(title = "Top-Windparks nach Anlagenzahl")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactRankingList(
            entries = detail.topParks,
            fullListTitle = "Rangliste: Windparks nach Anlagenzahl",
            onNavigateToParkDetail = onNavigateToParkDetail,
            onNavigateToRegionDetail = onNavigateToRegionDetail,
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Annahmen")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactAssumptionsFooter(assumptions = detail.assumptions, qualityLabel = detail.qualityLabel)
    }
}

@Composable
private fun Co2DetailContent(
    detail: Co2ImpactDetail,
    onNavigateToParkDetail: (String) -> Unit,
    onNavigateToRegionDetail: (String, String) -> Unit,
) {
    ImpactSummaryBlock(
        label = "Geschätzte Einsparung",
        value = detail.summaryValue,
        subtitle = detail.summarySubtitle,
    )
    ImpactSectionCard {
        SectionTitle(title = "Was bedeutet das?")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Die Zahl wird aus geschätzter Jahresproduktion und einem Emissionsfaktor für den deutschen Strommix berechnet. Sie ist eine Modellrechnung, kein gemessener Wert.",
            color = DarkText,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Top-Windparks nach CO₂-Einsparung")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactRankingList(
            entries = detail.topParks,
            fullListTitle = "Rangliste: Windparks nach CO₂-Einsparung",
            onNavigateToParkDetail = onNavigateToParkDetail,
            onNavigateToRegionDetail = onNavigateToRegionDetail,
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Was heißt das ungefähr?")
        Spacer(modifier = Modifier.height(14.dp))
        FactList(items = detail.equivalents.map { FactListItem(it.label, "${it.value} · ${it.description}") })
        Text(
            text = "Grobe Vergleichswerte, gerundet auf Basis dokumentierter MVP-Annahmen.",
            color = MutedText,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    ImpactSectionCard {
        SectionTitle(title = "Annahmen")
        Spacer(modifier = Modifier.height(14.dp))
        ImpactAssumptionsFooter(assumptions = detail.assumptions, qualityLabel = detail.qualityLabel)
    }
}

private data class ImpactRankingEntry(
    val item: RankingItem,
    val target: ImpactNavigateTarget?,
)

private fun ImpactNavigateTarget?.rankingId(): String? = when (this) {
    is ImpactNavigateTarget.Park -> "park:$id"
    is ImpactNavigateTarget.Region -> "$type:$id"
    null -> null
}

private fun ImpactNavigateTarget?.subtitleLabel(): String = when (this) {
    is ImpactNavigateTarget.Park -> "Windpark"
    is ImpactNavigateTarget.Region -> when (type) {
        "city" -> "Gemeinde"
        "district" -> "Landkreis"
        "state" -> "Bundesland"
        else -> "Region"
    }
    null -> "Auswertung"
}

private fun List<StatsImpactFact>.toFactListItems(): List<FactListItem> =
    map { fact -> FactListItem(fact.label, fact.value) }

private fun String.impactDetailIcon(): ImageVector = when (this) {
    StatsImpactType.Households.name -> Icons.Outlined.Home
    StatsImpactType.MunicipalBenefit.name -> Icons.Outlined.Euro
    StatsImpactType.Turbines.name -> Icons.Outlined.WindPower
    StatsImpactType.Co2.name -> Icons.Outlined.Eco
    else -> Icons.Outlined.WindPower
}
