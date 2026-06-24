package app.feature.detail

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.model.Metric
import app.core.model.SnapshotAssumption
import app.core.model.WindTurbine
import app.core.ui.theme.WindklarTheme
import app.core.ui.components.StatusBadge
import app.core.ui.components.formatDataQuality
import app.core.ui.components.qualityColors
import app.core.ui.components.CitizenImpactDashboard
import app.core.ui.components.DataStatusFooter
import app.core.ui.components.ImpactMetric
import androidx.compose.ui.text.style.TextOverflow
import app.core.util.formatGermanNumber
import app.core.util.isRedundantMunicipality
import app.core.ui.components.WindklarHeader

private val ScreenBackground @Composable get() = WindklarTheme.colors.screenBackground
private val PrimaryGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val HeaderEndGreen @Composable get() = WindklarTheme.colors.headerEndGreen
private val DarkGreen @Composable get() = WindklarTheme.colors.darkGreen
private val MutedGreen @Composable get() = WindklarTheme.colors.mutedGreen
private val PaleGreen @Composable get() = WindklarTheme.colors.paleGreen


@Composable
fun ParkDetailScreen(
    viewModel: ParkDetailViewModel,
    onBack: () -> Unit,
    onNavigateToRegion: (type: String, id: String) -> Unit,
    onNavigateToCountry: () -> Unit,
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

    val park = uiState.park
    if (park == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Windpark konnte nicht geladen werden.", color = DarkGreen)
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
        WindklarHeader(
            title = park.name,
            subtitle = "Gemeinde ${park.municipalityName}",
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
            },
            breadcrumbs = {
                val breadcrumbSegments = listOfNotNull(
                    app.core.ui.components.BreadcrumbSegment(name = "Deutschland", onClick = onNavigateToCountry),
                    app.core.ui.components.BreadcrumbSegment(name = park.stateName, onClick = { onNavigateToRegion("state", park.stateId) }),
                    app.core.ui.components.BreadcrumbSegment(name = park.districtName, onClick = { onNavigateToRegion("district", park.districtId) }),
                    app.core.ui.components.BreadcrumbSegment(
                        name = "Gemeinde ${park.municipalityName}",
                        onClick = { onNavigateToRegion("city", park.municipalityId) }
                    ).takeUnless { isRedundantMunicipality(park.districtName, park.municipalityName) }
                )
                app.core.ui.components.Breadcrumbs(
                    segments = breadcrumbSegments,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            bottomPadding = 48.dp
        )

        // Content
        Column(
            modifier = Modifier
                .offset(y = (-32).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Summary Card
            SummaryCard(park = park)

            val prodMetric = uiState.metrics.firstOrNull { it.metricType == "annual_production" }
            val co2Metric = uiState.metrics.firstOrNull { it.metricType == "co2_savings" }
            val houseMetric = uiState.metrics.firstOrNull { it.metricType == "households_supplied" } ?: uiState.metrics.firstOrNull { it.metricType == "household_equivalent" }
            val muniMetric = uiState.metrics.firstOrNull { it.metricType == "municipal_participation" }

            val prodVal = prodMetric?.value?.let { "${formatGermanNumber(it / 1_000_000.0, 1)} GWh/Jahr" } ?: "Keine Daten"
            val co2Val = co2Metric?.value?.let { "${formatGermanNumber((it / 1000.0).toInt())} t/Jahr" } ?: "Keine Daten"
            val houseVal = houseMetric?.value?.let { "${formatGermanNumber(it.toInt())} Haushalte" } ?: "Keine Daten"
            val muniVal = muniMetric?.value?.let { "ca. ${formatNumber(it.toInt())} EUR/Jahr" } ?: "Keine Daten"

            val flh = uiState.assumptions.firstOrNull { it.id == "full_load_hours" }?.value ?: 2000.0
            val co2Factor = uiState.assumptions.firstOrNull { it.id == "emission_factor_kg_per_kwh" }?.value ?: 0.38
            val consumption = uiState.assumptions.firstOrNull { it.id == "household_consumption_kwh" }?.value ?: 3500.0
            val muniBenefit = uiState.assumptions.firstOrNull { it.id == "municipal_benefit_eur_per_kwh" }?.value ?: 0.002

            val prodNote = "Aus der installierten Leistung und der geschätzten Jahresproduktion berechnet. Entspricht standortspezifischen Volllaststunden von ${formatGermanNumber(flh.toInt())} h/a (bundesweiter Richtwert: 2.000 h/a)."
            val co2Note = "Berechnet aus der Jahresproduktion und dem CO₂-Emissionsfaktor des deutschen Strommixes von ${formatGermanNumber(co2Factor * 1000.0, 0)} g/kWh (bzw. ${formatGermanNumber(co2Factor, 2)} kg/kWh)."
            val houseNote = "Rechnerische Abdeckung von privaten Haushalten basierend auf einem durchschnittlichen Stromverbrauch von ${formatGermanNumber(consumption.toInt())} kWh/Jahr pro Haushalt."
            val muniNote = "Schätzung nach § 6 EEG für Windenergie an Land. Grundlage: ${formatGermanNumber(muniBenefit * 100.0, 1)} ct/kWh der geschätzten Jahresproduktion."

            val impactMetrics = listOf(
                ImpactMetric(
                    label = "Jahresproduktion",
                    value = prodVal,
                    isMissing = prodMetric?.value == null,
                    note = prodNote,
                    icon = Icons.Outlined.Bolt
                ),
                ImpactMetric(
                    label = "Vermiedene CO2-Emissionen",
                    value = co2Val,
                    isMissing = co2Metric?.value == null,
                    note = co2Note,
                    icon = Icons.Outlined.Eco
                ),
                ImpactMetric(
                    label = "Versorgte Haushalte",
                    value = houseVal,
                    isMissing = houseMetric?.value == null,
                    note = houseNote,
                    icon = Icons.Outlined.Home
                ),
                ImpactMetric(
                    label = "Kommunale Beteiligung an Land (§6 EEG)",
                    value = muniVal,
                    isMissing = muniMetric?.value == null,
                    note = muniNote,
                    icon = Icons.Outlined.MonetizationOn
                )
            )

            // Citizen Impact Dashboard
            CitizenImpactDashboard(
                metrics = impactMetrics
            )

            // Individual Turbines List
            TurbinesSection(turbines = uiState.turbines)

            // Subtle Footer
            DataStatusFooter(dataQuality = park.dataQuality)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    park: app.core.model.WindPark
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Allgemeine Info",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Anlagenanzahl", color = MutedGreen, fontSize = 12.sp)
                    Text(formatTurbineCount(park.turbineCount), color = DarkGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gesamtleistung", color = MutedGreen, fontSize = 12.sp)
                    val capStr = park.installedCapacityKw?.let { "${formatGermanNumber(it / 1000.0, 1)} MW" } ?: "k.A."
                    Text(capStr, color = DarkGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Shared UI components used instead of local duplicates

@Composable
private fun TurbinesSection(turbines: List<WindTurbine>) {
    var turbinesExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Windenergieanlagen (${turbines.size})",
            color = DarkGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (turbines.isEmpty()) {
            Text("Keine detaillierten Anlagendaten vorhanden.", color = MutedGreen, fontSize = 14.sp)
        } else {
            val visibleTurbines = if (turbinesExpanded) turbines else turbines.take(3)
            visibleTurbines.forEach { turbine ->
                TurbineCard(turbine = turbine)
            }

            if (turbines.size > 3) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { turbinesExpanded = !turbinesExpanded },
                    shape = RoundedCornerShape(8.dp),
                    color = PaleGreen,
                    contentColor = PrimaryGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (turbinesExpanded) "Weniger Anlagen anzeigen" else "Alle Anlagen anzeigen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TurbineCard(turbine: WindTurbine) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = turbine.name.ifBlank { "Anlage #${turbine.id.takeLast(4)}" },
                    color = DarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    text = turbine.status ?: "Unbekannt",
                    containerColor = if (turbine.status?.lowercase() == "in betrieb") WindklarTheme.colors.paleGreen else WindklarTheme.colors.statusOrangeLight,
                    contentColor = if (turbine.status?.lowercase() == "in betrieb") WindklarTheme.colors.primaryGreen else WindklarTheme.colors.statusOrangeDark,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hersteller / Modell", color = MutedGreen, fontSize = 11.sp)
                    val modelStr = if (!turbine.manufacturer.isNullOrBlank()) {
                        "${turbine.manufacturer} ${turbine.model ?: ""}".trim()
                    } else null
                    Text(modelStr ?: "k.A.", color = DarkGreen, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nennleistung", color = MutedGreen, fontSize = 11.sp)
                    val powStr = turbine.installedCapacityKw?.let { "${formatGermanNumber(it / 1000.0, 1)} MW" } ?: "k.A."
                    Text(powStr, color = DarkGreen, fontSize = 13.sp)
                }
            }

            val hubHeight = turbine.hubHeightM
            val rotorDiameter = turbine.rotorDiameterM
            if (hubHeight != null || rotorDiameter != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (hubHeight != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nabenhöhe", color = MutedGreen, fontSize = 11.sp)
                            Text("${formatGermanNumber(hubHeight, 1)} m", color = DarkGreen, fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (rotorDiameter != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rotordurchmesser", color = MutedGreen, fontSize = 11.sp)
                            Text("${formatGermanNumber(rotorDiameter, 1)} m", color = DarkGreen, fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            turbine.commissioningYear?.let { year ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inbetriebnahme", color = MutedGreen, fontSize = 11.sp)
                        Text(year.toString(), color = DarkGreen, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatNumber(number: Int): String = formatGermanNumber(number)

private fun formatAssumptionValue(value: Double): String = formatGermanNumber(value, 2, true)

private fun formatTurbineCount(count: Int): String {
    val unit = if (count == 1) "Windrad" else "Windräder"
    return "${formatGermanNumber(count)} $unit"
}
