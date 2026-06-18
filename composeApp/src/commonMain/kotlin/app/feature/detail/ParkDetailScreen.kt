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
import app.core.model.WindTurbine

private val ScreenBackground = Color(0xFFF8FAF7)
private val PrimaryGreen = Color(0xFF2D5A2D)
private val HeaderEndGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF1A3A1A)
private val MutedGreen = Color(0xFF5A7A5A)
private val PaleGreen = Color(0xFFE8F5E9)
private val HeartRed = Color(0xFFE53935)

@Composable
fun ParkDetailScreen(
    viewModel: ParkDetailViewModel,
    onBack: () -> Unit,
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
                turbineCount = park.turbineCount,
                installedCapacityKw = park.installedCapacityKw,
                sourceName = park.sourceName,
                dataQuality = park.dataQuality
            )

            // Citizen Impact Dashboard
            CitizenImpactDashboard(metrics = uiState.metrics)

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
    turbineCount: Int,
    installedCapacityKw: Long?,
    sourceName: String,
    dataQuality: String
) {
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
                text = "Allgemeine Info",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Anlagenanzahl", color = MutedGreen, fontSize = 12.sp)
                    Text(formatTurbineCount(turbineCount), color = DarkGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gesamtleistung", color = MutedGreen, fontSize = 12.sp)
                    val capStr = installedCapacityKw?.let { "${(it / 1000.0).roundTo(1)} MW" } ?: "k.A."
                    Text(capStr, color = DarkGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PaleGreen
                ) {
                    Text(
                        text = "Stammdaten: ${formatDataQuality(dataQuality)}",
                        color = PrimaryGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text("Quelle: $sourceName", color = MutedGreen, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CitizenImpactDashboard(metrics: List<Metric>) {
    val prodMetric = metrics.firstOrNull { it.metricType == "annual_production" }
    val co2Metric = metrics.firstOrNull { it.metricType == "co2_savings" }
    val houseMetric = metrics.firstOrNull { it.metricType == "households_supplied" } ?: metrics.firstOrNull { it.metricType == "household_equivalent" }
    val muniMetric = metrics.firstOrNull { it.metricType == "municipal_participation" }

    val prodVal = prodMetric?.value?.let { "${(it / 1_000_000.0).roundTo(1)} GWh/Jahr" } ?: "Keine Daten"
    val co2Val = co2Metric?.value?.let { "${formatNumber((it / 1000.0).toInt())} t/Jahr" } ?: "Keine Daten"
    val houseVal = houseMetric?.value?.let { "${formatNumber(it.toInt())} Haushalte" } ?: "Keine Daten"
    val muniVal = muniMetric?.value?.let { "ca. ${formatNumber(it.toInt())} EUR/Jahr" } ?: "Keine Daten"

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
                quality = prodMetric?.dataQuality ?: "estimated"
            )

            ImpactRow(
                icon = Icons.Outlined.Eco,
                label = "Vermeidbare CO2-Emissionen",
                value = co2Val,
                note = co2Metric?.calculationNote,
                quality = co2Metric?.dataQuality ?: "estimated"
            )

            ImpactRow(
                icon = Icons.Outlined.Home,
                label = "Versorgte Haushalte",
                value = houseVal,
                note = houseMetric?.calculationNote,
                quality = houseMetric?.dataQuality ?: "estimated"
            )

            ImpactRow(
                icon = Icons.Outlined.MonetizationOn,
                label = "Kommunale Beteiligung (§6 EEG)",
                value = muniVal,
                note = "Geschätzte Beteiligung von ca. 0,2 ct/kWh der Jahresproduktion. Achtung: Keine bestätigte Auszahlung.",
                quality = muniMetric?.dataQuality ?: "estimated"
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = DarkGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (quality == "official") Color(0xFFC8E6C9) else Color(0xFFFFECB3)
                ) {
                    Text(
                        text = formatDataQuality(quality).uppercase(),
                        color = if (quality == "official") Color(0xFF2D5A2D) else Color(0xFFB78103),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
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
        color = Color.White,
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
                    fontSize = 15.sp
                )
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (turbine.status?.lowercase() == "in betrieb") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = turbine.status ?: "Unbekannt",
                        color = if (turbine.status?.lowercase() == "in betrieb") Color(0xFF2D5A2D) else Color(0xFFE65100),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hersteller / Modell", color = MutedGreen, fontSize = 11.sp)
                    val modelStr = if (!turbine.manufacturer.isNullOrBlank()) "${turbine.manufacturer} ${turbine.model ?: ""}" else "k.A."
                    Text(modelStr, color = DarkGreen, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nennleistung", color = MutedGreen, fontSize = 11.sp)
                    val powStr = turbine.installedCapacityKw?.let { "${(it / 1000.0).roundTo(1)} MW" } ?: "k.A."
                    Text(powStr, color = DarkGreen, fontSize = 13.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nabenhöhe", color = MutedGreen, fontSize = 11.sp)
                    val heightStr = turbine.hubHeightM?.let { "${it.roundTo(1)} m" } ?: "k.A."
                    Text(heightStr, color = DarkGreen, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rotordurchmesser", color = MutedGreen, fontSize = 11.sp)
                    val diamStr = turbine.rotorDiameterM?.let { "${it.roundTo(1)} m" } ?: "k.A."
                    Text(diamStr, color = DarkGreen, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DataSourceAttributionCard(attribution: String) {
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
                    text = "Berechnete Werte beruhen auf typischen Durchschnittsannahmen. Regionale Unterschiede (wie tatsächliche Windgeschwindigkeiten) können zu Abweichungen führen. Quelle: $attribution.",
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

private fun formatNumber(number: Int): String {
    return number.toString().reversed().chunked(3).joinToString(".").reversed()
}

private fun formatTurbineCount(count: Int): String {
    val unit = if (count == 1) "Windrad" else "Windräder"
    return "$count $unit"
}

private fun formatDataQuality(quality: String): String = when (quality.lowercase()) {
    "official" -> "Offiziell"
    "measured" -> "Gemessen"
    "derived" -> "Abgeleitet"
    "estimated" -> "Geschätzt"
    "simulated" -> "Simuliert"
    "missing" -> "Fehlend"
    else -> quality
}
