package app.feature.detail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.model.SnapshotAssumption
import app.core.model.WindPark
import app.core.ui.components.LabelWithBadge
import app.core.ui.components.formatDataQuality
import app.core.ui.components.qualityColors
import app.core.ui.components.RankingList

private val ScreenBackground = Color(0xFFF8FAF7)
private val PrimaryGreen = Color(0xFF2D5A2D)
private val HeaderEndGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF1A3A1A)
private val MutedGreen = Color(0xFF5A7A5A)
private val PaleGreen = Color(0xFFE8F5E9)
private val TrackGreen = Color(0xFFDDEBDD)
private val HeartRed = Color(0xFFE53935)

@Composable
fun RegionDetailScreen(
    viewModel: RegionDetailViewModel,
    onBack: () -> Unit,
    onParkSelected: (String) -> Unit,
    onRegionSelected: (String, String) -> Unit,
) {
    val uiState = viewModel.uiState

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
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
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 48.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(onClick = viewModel::toggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (uiState.isFavorite) HeartRed else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = uiState.regionName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = uiState.regionTypeLabel,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Parent hierarchy links if available
            if (uiState.parentDistrictId != null || uiState.parentStateId != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.parentDistrictId?.let { distId ->
                        RegionChip(
                            label = "Kreis: ${uiState.parentDistrictName}",
                            onClick = { onRegionSelected("district", distId) }
                        )
                    }
                    uiState.parentStateId?.let { stateId ->
                        RegionChip(
                            label = "Land: ${uiState.parentStateName}",
                            onClick = { onRegionSelected("state", stateId) }
                        )
                    }
                }
            }
        }

        // Content Area
        Column(
            modifier = Modifier
                .offset(y = (-32).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Summary Card
            RegionSummaryCard(
                windParkCount = uiState.windParkCount,
                turbineCount = uiState.turbineCount,
                installedCapacityMw = uiState.installedCapacityMw,
                shareOfStateCapacity = uiState.shareOfStateCapacity,
                shareOfNationalCapacity = uiState.shareOfNationalCapacity,
                parentStateName = uiState.parentStateName ?: uiState.regionName
            )

            // Aggregated Citizen Impact Dashboard
            RegionCitizenImpactDashboard(
                productionGwh = uiState.annualProductionGwh,
                co2SavingsTons = uiState.co2SavingsTons,
                householdsSupplied = uiState.householdsSupplied,
                municipalBenefitEur = uiState.municipalBenefitEur,
                regionTypeLabel = uiState.regionTypeLabel
            )

            // Display ranking of sub-regions for State and District, and flat list of parks for City
            if (uiState.regionType.lowercase() == "city") {
                WindParksSection(
                    windParks = uiState.windParks,
                    onParkSelected = onParkSelected
                )
            } else {
                SubRegionsSection(
                    regionType = uiState.regionType,
                    rankings = uiState.subRegionRankings,
                    onDetailsClick = { subRegionId ->
                        val subType = if (uiState.regionType.lowercase() == "state") "district" else "city"
                        onRegionSelected(subType, subRegionId)
                    }
                )
            }

            // Calculation assumptions
            CalculationAssumptionsCard(assumptions = uiState.assumptions)

            // Data quality limitations notice
            DataSourceAttributionCard(attribution = uiState.attribution)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RegionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.2f),
        contentColor = Color.White
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RegionSummaryCard(
    windParkCount: Int,
    turbineCount: Int,
    installedCapacityMw: Double,
    shareOfStateCapacity: Float?,
    shareOfNationalCapacity: Float,
    parentStateName: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Übersicht",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Windparks", color = MutedGreen, fontSize = 12.sp)
                    Text(
                        text = "$windParkCount Park${if (windParkCount == 1) "" else "s"}",
                        color = DarkGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Windräder", color = MutedGreen, fontSize = 12.sp)
                    Text(
                        text = "$turbineCount Anlage${if (turbineCount == 1) "" else "n"}",
                        color = DarkGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("Gesamtleistung", color = MutedGreen, fontSize = 12.sp)
                    Text(
                        text = "${installedCapacityMw.roundTo(1).toString().replace(".", ",")} MW",
                        color = DarkGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Share of state capacity (only for city and district)
            if (shareOfStateCapacity != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Anteil am Bundesland ($parentStateName)",
                            color = MutedGreen,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatPercent(shareOfStateCapacity),
                            color = DarkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { shareOfStateCapacity },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryGreen,
                        trackColor = TrackGreen
                    )
                }
            }

            // Share of national capacity
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Anteil am Bund (Deutschland)",
                        color = MutedGreen,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatPercent(shareOfNationalCapacity),
                        color = DarkGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { shareOfNationalCapacity },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryGreen,
                    trackColor = TrackGreen
                )
            }
        }
    }
}

@Composable
private fun RegionCitizenImpactDashboard(
    productionGwh: Double,
    co2SavingsTons: Double,
    householdsSupplied: Int,
    municipalBenefitEur: Double?,
    regionTypeLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Regionale Klimawirkung",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            RegionImpactRow(
                icon = Icons.Outlined.Bolt,
                label = "Jahresproduktion",
                value = "${productionGwh.roundTo(1).toString().replace(".", ",")} GWh/Jahr",
                note = "Geschätzte Stromerzeugung pro Jahr aller Windparks in dieser Region.",
                quality = "estimated"
            )

            RegionImpactRow(
                icon = Icons.Outlined.Eco,
                label = "Vermiedenes CO2",
                value = "${formatNumber(co2SavingsTons.toInt())} t/Jahr",
                note = "Eingesparter CO2-Ausstoß im deutschen Strommix pro Jahr.",
                quality = "estimated"
            )

            RegionImpactRow(
                icon = Icons.Outlined.Home,
                label = "Versorgte Haushalte",
                value = "${formatNumber(householdsSupplied)} Haushalte",
                note = "Rechnerische Abdeckung privater 3-Personen-Haushalte (3.500 kWh/Jahr).",
                quality = "estimated"
            )

            municipalBenefitEur?.let { benefit ->
                val muniLabel = if (regionTypeLabel.lowercase() == "gemeinde") {
                    "Kommunale Beteiligung an Land (§6 EEG)"
                } else {
                    "Mögliche kommunale Beteiligung an Land (§6 EEG)"
                }
                val muniNote = if (regionTypeLabel.lowercase() == "gemeinde") {
                    "Geschätzte mögliche kommunale Beteiligung für Windenergie an Land nach §6 EEG (0,2 ct/kWh) für das Budget dieser Gemeinde. Keine Gewähr für tatsächliche Verträge."
                } else {
                    "Aggregierte mögliche kommunale Beteiligung für Windenergie an Land nach §6 EEG (0,2 ct/kWh) für die in dieser Region liegenden Gemeinden."
                }

                RegionImpactRow(
                    icon = Icons.Outlined.MonetizationOn,
                    label = muniLabel,
                    value = "ca. ${formatNumber(benefit.toInt())} EUR/Jahr",
                    note = muniNote,
                    quality = "estimated"
                )
            }
        }
    }
}

@Composable
private fun RegionImpactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    note: String,
    quality: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(PaleGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            LabelWithBadge(label = label, quality = quality, labelColor = DarkGreen)
            Text(value, color = PrimaryGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            Text(
                text = note,
                color = MutedGreen,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SubRegionsSection(
    regionType: String,
    rankings: List<app.core.model.RankingItem>,
    onDetailsClick: (String) -> Unit
) {
    val sectionTitle = when (regionType.lowercase()) {
        "state" -> "Landkreise in diesem Bundesland (${rankings.size})"
        "district" -> "Gemeinden in diesem Landkreis (${rankings.size})"
        else -> "Regionen in dieser Ansicht (${rankings.size})"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = sectionTitle,
            color = DarkGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                RankingList(
                    values = rankings,
                    onDetailsClick = onDetailsClick,
                    onActionClick = null
                )
            }
        }
    }
}

@Composable
private fun WindParksSection(
    windParks: List<WindPark>,
    onParkSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Windparks in dieser Region (${windParks.size})",
            color = DarkGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        windParks.forEach { park ->
            val qualityColor = qualityColors(park.dataQuality)
            val capMw = (park.installedCapacityKw ?: 0L) / 1000.0
            
            Surface(
                onClick = { onParkSelected(park.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = park.name,
                            color = DarkGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${park.turbineCount} Anlage${if (park.turbineCount == 1) "" else "n"}",
                                color = MutedGreen,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${capMw.roundTo(1).toString().replace(".", ",")} MW",
                                color = MutedGreen,
                                fontSize = 12.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = qualityColor.container,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Stammdaten: ${formatDataQuality(park.dataQuality)}",
                                color = qualityColor.content,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Öffnen",
                        tint = MutedGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculationAssumptionsCard(assumptions: List<SnapshotAssumption>) {
    val orderedAssumptionIds = listOf(
        "full_load_hours",
        "emission_factor_kg_per_kwh",
        "household_consumption_kwh",
        "municipal_benefit_eur_per_kwh",
    )
    val visibleAssumptions = orderedAssumptionIds.mapNotNull { id ->
        assumptions.firstOrNull { it.id == id }
    }

    if (visibleAssumptions.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Welche Annahmen stecken dahinter?",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Diese Werte erklären, wie WindKlar die geschätzten Wirkungswerte berechnet.",
                color = MutedGreen,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            visibleAssumptions.forEach { assumption ->
                AssumptionRow(assumption = assumption)
            }
        }
    }
}

@Composable
private fun AssumptionRow(assumption: SnapshotAssumption) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = assumption.label,
                color = DarkGreen,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${formatAssumptionValue(assumption.value)} ${assumption.unit}",
                color = PrimaryGreen,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        if (!assumption.calculationNote.isNullOrBlank()) {
            Text(
                text = assumption.calculationNote,
                color = MutedGreen,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun DataSourceAttributionCard(attribution: String) {
    val normalizedAttribution = attribution.removePrefix("Quelle:").trim().ifBlank {
        "Marktstammdatenregister der Bundesnetzagentur"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF9C4),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBC02D))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = Color(0xFFF57F17),
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = "Datenhinweis",
                    color = Color(0xFFF57F17),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Berechnete Werte beruhen auf typischen Durchschnittsannahmen. Regionale Unterschiede können zu Abweichungen führen. Quelle: $normalizedAttribution.",
                    color = Color(0xFF5D4037),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

private fun Float.roundTo(decimals: Int): Float = this.toDouble().roundTo(decimals).toFloat()

private fun formatNumber(number: Int): String {
    return number.toString().reversed().chunked(3).joinToString(".").reversed()
}

private fun formatAssumptionValue(value: Double): String {
    val roundedInt = value.toInt()
    return if (value == roundedInt.toDouble()) {
        formatNumber(roundedInt)
    } else {
        value.toString().replace(".", ",")
    }
}

private fun formatPercent(value: Float): String {
    val pct = value * 100
    val rounded = pct.roundTo(1)
    return "${rounded.toString().replace(".", ",")}%"
}
