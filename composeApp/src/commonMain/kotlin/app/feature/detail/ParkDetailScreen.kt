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
import androidx.compose.runtime.Composable
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
import app.core.ui.components.LabelWithBadge
import app.core.ui.components.StatusBadge
import app.core.ui.components.formatDataQuality
import app.core.ui.components.qualityColors
import androidx.compose.ui.text.style.TextOverflow
import app.core.util.formatGermanNumber

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
                        tint = if (uiState.isFavorite) WindklarTheme.colors.heartRed else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val breadcrumbSegments = listOf(
                app.core.ui.components.BreadcrumbSegment(name = "Deutschland", onClick = onNavigateToCountry),
                app.core.ui.components.BreadcrumbSegment(name = park.stateName, onClick = { onNavigateToRegion("state", park.stateId) }),
                app.core.ui.components.BreadcrumbSegment(name = park.districtName, onClick = { onNavigateToRegion("district", park.districtId) }),
                app.core.ui.components.BreadcrumbSegment(name = "Gemeinde ${park.municipalityName}", onClick = { onNavigateToRegion("city", park.municipalityId) })
            )
            app.core.ui.components.Breadcrumbs(
                segments = breadcrumbSegments,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Text(
                text = park.name,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Gemeinde ${park.municipalityName}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .offset(y = (-32).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Summary Card
            SummaryCard(
                park = park,
                onNavigateToRegion = onNavigateToRegion
            )

            // Citizen Impact Dashboard
            CitizenImpactDashboard(metrics = uiState.metrics)

            CalculationAssumptionsCard(assumptions = uiState.assumptions)

            // Individual Turbines List
            TurbinesSection(turbines = uiState.turbines)

            // Data quality limitations notice
            DataSourceAttributionCard(attribution = uiState.attribution)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    park: app.core.model.WindPark,
    onNavigateToRegion: (type: String, id: String) -> Unit
) {
    val quality = qualityColors(park.dataQuality)
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

            // Clickable region tags
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Regionen (tippen für Details)", color = MutedGreen, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RegionDetailChip(
                        label = park.municipalityName,
                        onClick = { onNavigateToRegion("city", park.municipalityId) },
                        modifier = Modifier.weight(1f)
                    )
                    RegionDetailChip(
                        label = park.districtName,
                        onClick = { onNavigateToRegion("district", park.districtId) },
                        modifier = Modifier.weight(1f)
                    )
                    RegionDetailChip(
                        label = park.stateName,
                        onClick = { onNavigateToRegion("state", park.stateId) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = quality.container
                ) {
                    Text(
                        text = "Stammdaten: ${formatDataQuality(park.dataQuality)}",
                        color = quality.content,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionDetailChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = PaleGreen,
        contentColor = PrimaryGreen,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CitizenImpactDashboard(metrics: List<Metric>) {
    val prodMetric = metrics.firstOrNull { it.metricType == "annual_production" }
    val co2Metric = metrics.firstOrNull { it.metricType == "co2_savings" }
    val houseMetric = metrics.firstOrNull { it.metricType == "households_supplied" } ?: metrics.firstOrNull { it.metricType == "household_equivalent" }
    val muniMetric = metrics.firstOrNull { it.metricType == "municipal_participation" }

    val prodVal = prodMetric?.value?.let { "${formatGermanNumber(it / 1_000_000.0, 1)} GWh/Jahr" } ?: "Keine Daten"
    val co2Val = co2Metric?.value?.let { "${formatGermanNumber((it / 1000.0).toInt())} t/Jahr" } ?: "Keine Daten"
    val houseVal = houseMetric?.value?.let { "${formatGermanNumber(it.toInt())} Haushalte" } ?: "Keine Daten"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = WindklarTheme.colors.cardBackground,
                shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Lokaler Nutzen & Klimawirkung",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            ImpactRow(
                icon = Icons.Outlined.Bolt,
                label = "Jahresproduktion",
                value = prodVal,
                note = prodMetric?.calculationNote,
                quality = prodMetric?.dataQuality ?: "missing"
            )

            ImpactRow(
                icon = Icons.Outlined.Eco,
                label = "Vermiedene CO2-Emissionen",
                value = co2Val,
                note = co2Metric?.calculationNote,
                quality = co2Metric?.dataQuality ?: "missing"
            )

            ImpactRow(
                icon = Icons.Outlined.Home,
                label = "Versorgte Haushalte",
                value = houseVal,
                note = houseMetric?.calculationNote,
                quality = houseMetric?.dataQuality ?: "missing"
            )

            muniMetric?.let { metric ->
                ImpactRow(
                    icon = Icons.Outlined.MonetizationOn,
                    label = "Kommunale Beteiligung an Land (§6 EEG)",
                    value = metric.value?.let { "ca. ${formatNumber(it.toInt())} EUR/Jahr" } ?: "Keine Daten",
                    note = metric.calculationNote
                        ?: "Schätzung nach § 6 EEG für Windenergie an Land. Grundlage: 0,2 ct/kWh und geschätzte Jahresproduktion. Keine bestätigte Auszahlung.",
                    quality = metric.dataQuality
                )
            }
        }
    }
}

@Composable
private fun ImpactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    note: String?,
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
            if (!note.isNullOrBlank()) {
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
        color = WindklarTheme.colors.cardBackground,
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
private fun TurbinesSection(turbines: List<WindTurbine>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            turbines.forEach { turbine ->
                TurbineCard(turbine = turbine)
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
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    text = turbine.status ?: "Unbekannt",
                    containerColor = if (turbine.status?.lowercase() == "in betrieb") WindklarTheme.colors.paleGreen else Color(0xFFFFF3E0),
                    contentColor = if (turbine.status?.lowercase() == "in betrieb") WindklarTheme.colors.primaryGreen else Color(0xFFE65100),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hersteller / Modell", color = MutedGreen, fontSize = 11.sp)
                    val modelStr = if (!turbine.manufacturer.isNullOrBlank()) "${turbine.manufacturer} ${turbine.model ?: ""}" else "k.A."
                    Text(modelStr, color = DarkGreen, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nennleistung", color = MutedGreen, fontSize = 11.sp)
                    val powStr = turbine.installedCapacityKw?.let { "${formatGermanNumber(it / 1000.0, 1)} MW" } ?: "k.A."
                    Text(powStr, color = DarkGreen, fontSize = 13.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nabenhöhe", color = MutedGreen, fontSize = 11.sp)
                    val heightStr = turbine.hubHeightM?.let { "${formatGermanNumber(it, 1)} m" } ?: "k.A."
                    Text(heightStr, color = DarkGreen, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rotordurchmesser", color = MutedGreen, fontSize = 11.sp)
                    val diamStr = turbine.rotorDiameterM?.let { "${formatGermanNumber(it, 1)} m" } ?: "k.A."
                    Text(diamStr, color = DarkGreen, fontSize = 13.sp)
                }
            }

            val qualityColor = qualityColors(turbine.dataQuality)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = qualityColor.container
                ) {
                    Text(
                        text = "Stammdaten: ${formatDataQuality(turbine.dataQuality)}",
                        color = qualityColor.content,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
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
        color = WindklarTheme.colors.warningYellowLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, WindklarTheme.colors.warningAmber)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = WindklarTheme.colors.warningAmberDark,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = "Datenhinweis",
                    color = WindklarTheme.colors.warningAmberDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Berechnete Werte beruhen auf typischen Durchschnittsannahmen. Regionale Unterschiede können zu Abweichungen führen. Quelle: $normalizedAttribution.",
                    color = WindklarTheme.colors.warningBrown,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
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
