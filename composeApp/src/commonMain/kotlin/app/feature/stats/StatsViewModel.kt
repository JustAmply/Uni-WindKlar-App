package app.feature.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.WindPark
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch
import kotlin.math.round

private const val DEFAULT_FULL_LOAD_HOURS = 2_000.0
private const val CO2_PER_BERLIN_NYC_FLIGHT_KG = 1_000.0
private const val CO2_PER_CAR_YEAR_KG = 1_500.0
private const val CO2_PER_COAL_PLANT_YEAR_KG = 1_250_000_000.0

class StatsViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState by mutableStateOf(StatsUiState())
        private set

    init {
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val parks = repository.getWindParks()
            val turbines = repository.getAllWindTurbines()
            val nationalMetrics = repository.getMetricsForNational()
            val recentParks = repository.getRecentWindParks(limit = 1)
            val assumptions = repository.getSnapshotAssumptions()
            val attribution = repository.getSnapshotAttribution()

            val totalCapacityKw = parks.sumOf { it.installedCapacityKw ?: 0L }
            val totalCapacityMw = totalCapacityKw / 1_000.0
            val totalProductionKwh = nationalMetrics.firstValue("annual_production")
                ?: totalCapacityKw * (assumptions.firstOrNull { it.id == "full_load_hours" }?.value ?: DEFAULT_FULL_LOAD_HOURS)
            val totalCo2Kg = nationalMetrics.firstValue("co2_savings") ?: 0.0
            val totalHouseholds = nationalMetrics.firstValue("household_equivalent") ?: 0.0
            val totalMunicipalBenefit = nationalMetrics.firstValue("municipal_participation") ?: 0.0

            val districts = buildDistrictStats(parks, turbines.size, totalCapacityMw)
            val selectedDistrict = selectDistrictComparison(
                districts = districts,
                recentPark = recentParks.firstOrNull(),
            )

            uiState = StatsUiState(
                subtitle = "Deutschland · Snapshot 2025",
                overviewCards = listOf(
                    StatsOverviewCard(
                        value = formatInteger(parks.size),
                        label = "Windparks",
                        icon = StatsIcon.Wind,
                    ),
                    StatsOverviewCard(
                        value = formatEnergy(totalProductionKwh),
                        label = "Produktion",
                        icon = StatsIcon.Production,
                    ),
                    StatsOverviewCard(
                        value = formatCapacity(totalCapacityMw),
                        label = "Leistung",
                        icon = StatsIcon.Capacity,
                    ),
                ),
                impactCards = listOf(
                    StatsImpactCard(
                        title = "Haushalte",
                        value = formatCompact(totalHouseholds),
                        description = "rechnerisch mit Windstrom versorgt",
                        quality = "estimated",
                        icon = StatsIcon.Household,
                    ),
                    StatsImpactCard(
                        title = "Kommunaler Nutzen",
                        value = formatCurrency(totalMunicipalBenefit),
                        description = "mögliche Beteiligung nach § 6 EEG",
                        quality = "estimated",
                        icon = StatsIcon.Money,
                    ),
                    StatsImpactCard(
                        title = "Anlagen",
                        value = formatInteger(turbines.size),
                        description = "MaStR/Open-MaStR-Stammdaten im Snapshot",
                        quality = "official",
                        icon = StatsIcon.Wind,
                    ),
                    StatsImpactCard(
                        title = "CO2 gespart",
                        value = formatCo2(totalCo2Kg),
                        description = "vermiedene Emissionen pro Jahr",
                        quality = "estimated",
                        icon = StatsIcon.Co2,
                    ),
                ),
                topDistricts = districts.take(5),
                districtComparison = selectedDistrict,
                co2Summary = formatCo2(totalCo2Kg),
                co2Comparisons = buildCo2Comparisons(totalCo2Kg),
                capacityClasses = buildCapacityClasses(parks),
                qualityNotes = listOf(
                    StatsQualityNote(
                        label = "Windanlagen",
                        quality = "official",
                        description = "Stammdaten aus MaStR/Open-MaStR.",
                    ),
                    StatsQualityNote(
                        label = "Windparks",
                        quality = "derived",
                        description = "Gruppierung wird in der Vorverarbeitung aus Anlagendaten gebildet.",
                    ),
                    StatsQualityNote(
                        label = "Wirkungswerte",
                        quality = "estimated",
                        description = "Produktion, CO2 und kommunaler Nutzen beruhen auf dokumentierten MVP-Annahmen.",
                    ),
                ),
                attribution = attribution,
                isLoading = false,
            )
        }
    }

    private fun buildDistrictStats(
        parks: List<WindPark>,
        nationalTurbineCount: Int,
        totalCapacityMw: Double,
    ): List<DistrictStat> {
        val parkGroups = parks
            .filter { it.municipalityId.length >= 5 }
            .groupBy { it.municipalityId.take(5) }

        return parkGroups.map { (districtId, districtParks) ->
            val districtCapacityMw = districtParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
            val districtTurbines = districtParks.sumOf { it.turbineCount }
            val representativeMunicipality = districtParks
                .groupBy { it.municipalityName }
                .maxByOrNull { (_, municipalityParks) ->
                    municipalityParks.sumOf { it.installedCapacityKw ?: 0L }
                }
                ?.key
                ?: districtId
            DistrictStat(
                districtId = districtId,
                label = "AGS-Kreis $districtId",
                contextLabel = representativeMunicipality,
                windParkCount = districtParks.size,
                turbineCount = if (districtTurbines > 0) districtTurbines else nationalTurbineCount,
                installedCapacityMw = districtCapacityMw,
                shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                    (districtCapacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
            )
        }.sortedByDescending { it.installedCapacityMw }
    }

    private fun selectDistrictComparison(
        districts: List<DistrictStat>,
        recentPark: WindPark?,
    ): DistrictComparison? {
        if (districts.isEmpty()) return null

        val district = recentPark
            ?.municipalityId
            ?.takeIf { it.length >= 5 }
            ?.take(5)
            ?.let { districtId -> districts.firstOrNull { it.districtId == districtId } }
            ?: districts.first()

        val rank = districts.indexOfFirst { it.districtId == district.districtId } + 1
        val contextLabel = recentPark?.let { "Zuletzt geöffnet: Gemeinde ${it.municipalityName}" }
            ?: "Kein zuletzt geöffneter Park"

        return DistrictComparison(
            label = district.label,
            contextLabel = contextLabel,
            rankText = "Rang $rank von ${districts.size}",
            installedCapacity = formatCapacity(district.installedCapacityMw),
            windParks = formatInteger(district.windParkCount),
            turbines = formatInteger(district.turbineCount),
            nationalShare = formatPercent(district.shareOfNationalCapacity),
            shareProgress = district.shareOfNationalCapacity,
            isFallback = recentPark == null,
        )
    }

    private fun buildCo2Comparisons(totalCo2Kg: Double): List<Co2Comparison> {
        val values = listOf(
            "Flüge Berlin-NYC" to totalCo2Kg / CO2_PER_BERLIN_NYC_FLIGHT_KG,
            "Auto-Jahresfahrten" to totalCo2Kg / CO2_PER_CAR_YEAR_KG,
            "Kohlekraftwerksjahre" to totalCo2Kg / CO2_PER_COAL_PLANT_YEAR_KG,
        )
        return values.map { (label, value) ->
            Co2Comparison(
                label = label,
                value = when (label) {
                    "Kohlekraftwerksjahre" -> "ca. ${value.roundTo(0).formatGerman()} Jahre"
                    else -> "ca. ${formatCompact(value)}"
                },
                description = when (label) {
                    "Flüge Berlin-NYC" -> "als grobe Flug-Emissionseinordnung"
                    "Auto-Jahresfahrten" -> "auf Basis typischer Jahresfahrten"
                    else -> "bezogen auf ein großes Kohlekraftwerk"
                },
            )
        }
    }

    private fun buildCapacityClasses(parks: List<WindPark>): List<CapacityClassStat> {
        val classes = listOf(
            "< 5 MW" to parks.count { (it.installedCapacityKw ?: 0L) < 5_000L },
            "5-20 MW" to parks.count { (it.installedCapacityKw ?: 0L) in 5_000L until 20_000L },
            "20-50 MW" to parks.count { (it.installedCapacityKw ?: 0L) in 20_000L until 50_000L },
            "> 50 MW" to parks.count { (it.installedCapacityKw ?: 0L) >= 50_000L },
        )
        val maxCount = classes.maxOfOrNull { it.second } ?: 0
        return classes.map { (label, count) ->
            CapacityClassStat(
                label = label,
                count = count,
                share = if (maxCount > 0) count.toFloat() / maxCount else 0f,
            )
        }
    }

    private fun List<app.core.model.Metric>.firstValue(metricType: String): Double? =
        firstOrNull { it.metricType == metricType }?.value

    private fun formatInteger(value: Int): String = value.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()

    private fun formatEnergy(kwh: Double): String {
        val twh = kwh / 1_000_000_000.0
        return if (twh >= 1.0) {
            "${twh.roundTo(1).formatGerman()} TWh"
        } else {
            "${(kwh / 1_000_000.0).roundTo(1).formatGerman()} GWh"
        }
    }

    private fun formatCapacity(mw: Double): String =
        if (mw >= 1_000.0) {
            "${(mw / 1_000.0).roundTo(1).formatGerman()} GW"
        } else {
            "${mw.roundTo(0).formatGerman()} MW"
        }

    private fun formatCo2(kg: Double): String {
        val mioTons = kg / 1_000_000_000.0
        return "${mioTons.roundTo(1).formatGerman()} Mio. t"
    }

    private fun formatCurrency(value: Double): String =
        if (value >= 1_000_000_000.0) {
            "${(value / 1_000_000_000.0).roundTo(1).formatGerman()} Mrd. EUR"
        } else {
            "${(value / 1_000_000.0).roundTo(1).formatGerman()} Mio. EUR"
        }

    private fun formatCompact(value: Double): String =
        if (value >= 1_000_000.0) {
            "${(value / 1_000_000.0).roundTo(1).formatGerman()} Mio."
        } else {
            formatInteger(value.roundToInt())
        }

    private fun formatPercent(value: Float): String =
        "${(value * 100.0).roundTo(1).formatGerman()} %"

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    private fun Double.roundToInt(): Int = round(this).toInt()

    private fun Double.formatGerman(): String {
        val rounded = toString()
        val normalized = if (rounded.endsWith(".0")) rounded.dropLast(2) else rounded
        return normalized.replace(".", ",")
    }
}
