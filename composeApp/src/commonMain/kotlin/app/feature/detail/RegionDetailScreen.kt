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
import app.core.ui.components.formatDataQuality
import app.core.ui.components.qualityColors
import app.core.ui.components.RankingList
import app.core.ui.components.CitizenImpactDashboard
import app.core.ui.components.DataStatusFooter
import app.core.ui.components.ImpactMetric
import app.core.ui.theme.WindklarTheme
import app.core.util.formatGermanNumber



@Composable
fun RegionDetailScreen(
    viewModel: RegionDetailViewModel,
    onBack: () -> Unit,
    onParkSelected: (String) -> Unit,
    onRegionSelected: (String, String) -> Unit,
    onNavigateToCountry: () -> Unit,
) {
    val uiState = viewModel.uiState

    if (uiState.isLoading) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WindklarTheme.colors.screenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(WindklarTheme.colors.primaryGreen, WindklarTheme.colors.headerEndGreen),
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
                        tint = if (uiState.isFavorite) WindklarTheme.colors.heartRed else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val breadcrumbSegments = when (uiState.regionType.lowercase()) {
                "state" -> listOfNotNull(
                    app.core.ui.components.BreadcrumbSegment(name = "Deutschland", onClick = onNavigateToCountry)
                )
                "district" -> listOfNotNull(
                    app.core.ui.components.BreadcrumbSegment(name = "Deutschland", onClick = onNavigateToCountry),
                    uiState.parentStateName?.takeIf { it.isNotEmpty() }?.let { name ->
                        app.core.ui.components.BreadcrumbSegment(
                            name = name,
                            onClick = uiState.parentStateId?.let { id -> { onRegionSelected("state", id) } }
                        )
                    }
                )
                "city" -> listOfNotNull(
                    app.core.ui.components.BreadcrumbSegment(name = "Deutschland", onClick = onNavigateToCountry),
                    uiState.parentStateName?.takeIf { it.isNotEmpty() }?.let { name ->
                        app.core.ui.components.BreadcrumbSegment(
                            name = name,
                            onClick = uiState.parentStateId?.let { id -> { onRegionSelected("state", id) } }
                        )
                    },
                    uiState.parentDistrictName?.takeIf { it.isNotEmpty() }?.let { name ->
                        app.core.ui.components.BreadcrumbSegment(
                            name = name,
                            onClick = uiState.parentDistrictId?.let { id -> { onRegionSelected("district", id) } }
                        )
                    }
                )
                else -> listOfNotNull(
                    app.core.ui.components.BreadcrumbSegment(name = "Deutschland", onClick = onNavigateToCountry)
                )
            }
            app.core.ui.components.Breadcrumbs(
                segments = breadcrumbSegments,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Text(
                text = if (uiState.regionType.lowercase() == "district") cleanDistrictName(uiState.regionName) else uiState.regionName,
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

            val productionVal = "${formatGermanNumber(uiState.annualProductionGwh, 1)} GWh/Jahr"
            val co2SavingsVal = "${formatGermanNumber(uiState.co2SavingsTons.toInt())} t/Jahr"
            val householdsVal = "${formatGermanNumber(uiState.householdsSupplied)} Haushalte"
            val municipalBenefitVal = uiState.municipalBenefitEur?.let { "ca. ${formatGermanNumber(it.toInt())} EUR/Jahr" } ?: "Keine Daten"

            val flh = uiState.assumptions.firstOrNull { it.id == "full_load_hours" }?.value ?: 2000.0
            val co2Factor = uiState.assumptions.firstOrNull { it.id == "emission_factor_kg_per_kwh" }?.value ?: 0.38
            val consumption = uiState.assumptions.firstOrNull { it.id == "household_consumption_kwh" }?.value ?: 3500.0
            val muniBenefit = uiState.assumptions.firstOrNull { it.id == "municipal_benefit_eur_per_kwh" }?.value ?: 0.002

            val prodNote = "Geschätzte Stromerzeugung pro Jahr aller Windparks in dieser Region. Die zugrundeliegenden Volllaststunden der Parks betragen im Durchschnitt ${formatGermanNumber(flh.toInt())} h/a (bundesweiter Richtwert: 2.000 h/a)."
            val co2Note = "Berechnet aus der Jahresproduktion und dem CO₂-Emissionsfaktor des deutschen Strommixes von ${formatGermanNumber(co2Factor * 1000.0, 0)} g/kWh (bzw. ${formatGermanNumber(co2Factor, 2)} kg/kWh)."
            val houseNote = "Rechnerische Abdeckung von privaten Haushalten basierend auf einem durchschnittlichen Stromverbrauch von ${formatGermanNumber(consumption.toInt())} kWh/Jahr pro Haushalt."

            val muniLabel = if (uiState.regionTypeLabel.lowercase() == "gemeinde") {
                "Kommunale Beteiligung an Land (§6 EEG)"
            } else {
                "Mögliche kommunale Beteiligung an Land (§6 EEG)"
            }
            val muniNote = if (uiState.regionTypeLabel.lowercase() == "gemeinde") {
                "Geschätzte mögliche kommunale Beteiligung für Windenergie an Land nach § 6 EEG (Grundlage: ${formatGermanNumber(muniBenefit * 100.0, 1)} ct/kWh der Jahresproduktion) für das Budget dieser Gemeinde. Keine Gewähr für tatsächliche Verträge."
            } else {
                "Aggregierte mögliche kommunale Beteiligung für Windenergie an Land nach § 6 EEG (Grundlage: ${formatGermanNumber(muniBenefit * 100.0, 1)} ct/kWh der Jahresproduktion) für die in dieser Region liegenden Gemeinden."
            }

            val regionImpactMetrics = listOf(
                ImpactMetric(
                    label = "Jahresproduktion",
                    value = productionVal,
                    isMissing = uiState.annualProductionGwh == 0.0,
                    note = prodNote,
                    icon = Icons.Outlined.Bolt
                ),
                ImpactMetric(
                    label = "Vermiedenes CO2",
                    value = co2SavingsVal,
                    isMissing = uiState.co2SavingsTons == 0.0,
                    note = co2Note,
                    icon = Icons.Outlined.Eco
                ),
                ImpactMetric(
                    label = "Versorgte Haushalte",
                    value = householdsVal,
                    isMissing = uiState.householdsSupplied == 0,
                    note = houseNote,
                    icon = Icons.Outlined.Home
                ),
                ImpactMetric(
                    label = muniLabel,
                    value = municipalBenefitVal,
                    isMissing = uiState.municipalBenefitEur == null || uiState.municipalBenefitEur == 0.0,
                    note = muniNote,
                    icon = Icons.Outlined.MonetizationOn
                )
            )

            // Aggregated Citizen Impact Dashboard
            CitizenImpactDashboard(
                title = "Regionale Klimawirkung",
                metrics = regionImpactMetrics
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

            // Subtle Footer
            DataStatusFooter(dataQuality = null)

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
                color = WindklarTheme.colors.darkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Windparks", color = WindklarTheme.colors.mutedGreen, fontSize = 12.sp)
                    Text(
                        text = "$windParkCount Park${if (windParkCount == 1) "" else "s"}",
                        color = WindklarTheme.colors.darkGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Windräder", color = WindklarTheme.colors.mutedGreen, fontSize = 12.sp)
                    Text(
                        text = "${formatGermanNumber(turbineCount)} Anlage${if (turbineCount == 1) "" else "n"}",
                        color = WindklarTheme.colors.darkGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("Gesamtleistung", color = WindklarTheme.colors.mutedGreen, fontSize = 12.sp)
                    Text(
                        text = "${formatGermanNumber(installedCapacityMw, 1)} MW",
                        color = WindklarTheme.colors.darkGreen,
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
                            color = WindklarTheme.colors.mutedGreen,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatPercent(shareOfStateCapacity),
                            color = WindklarTheme.colors.darkGreen,
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
                        color = WindklarTheme.colors.primaryGreen,
                        trackColor = WindklarTheme.colors.trackGreen
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
                        color = WindklarTheme.colors.mutedGreen,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatPercent(shareOfNationalCapacity),
                        color = WindklarTheme.colors.darkGreen,
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
                    color = WindklarTheme.colors.primaryGreen,
                    trackColor = WindklarTheme.colors.trackGreen
                )
            }
        }
    }
}

// Reusable components are used instead of local duplicates

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
            color = WindklarTheme.colors.darkGreen,
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
            color = WindklarTheme.colors.darkGreen,
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
                            color = WindklarTheme.colors.darkGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Text(
                                 text = "${formatGermanNumber(park.turbineCount)} Anlage${if (park.turbineCount == 1) "" else "n"}",
                                 color = WindklarTheme.colors.mutedGreen,
                                 fontSize = 12.sp
                             )
                             Text(
                                 text = "${formatGermanNumber(capMw, 1)} MW",
                                 color = WindklarTheme.colors.mutedGreen,
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
                        tint = WindklarTheme.colors.mutedGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Reusable components are used instead of local duplicates

private fun cleanDistrictName(name: String): String {
    val prefixes = listOf("landkreis", "kreis", "stadtkreis", "regionalverband", "städteregion", "städte-region")
    var cleaned = name.trim()
    for (prefix in prefixes) {
        if (cleaned.lowercase().startsWith("$prefix ")) {
            cleaned = cleaned.substring(prefix.length + 1)
            break
        }
    }
    if (cleaned.lowercase().endsWith("-kreis")) {
        cleaned = cleaned.dropLast(6)
    }
    return cleaned.trim()
}

private fun formatNumber(number: Int): String = formatGermanNumber(number)

private fun formatAssumptionValue(value: Double): String = formatGermanNumber(value, 2, true)

private fun formatPercent(value: Float): String = "${formatGermanNumber(value * 100.0f, 1)}%"
