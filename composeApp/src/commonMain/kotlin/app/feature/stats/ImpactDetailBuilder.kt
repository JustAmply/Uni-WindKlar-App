package app.feature.stats

import app.core.model.Metric
import app.core.model.NationalStatsSummary
import app.core.model.RegionSummary
import app.core.model.SnapshotAssumption
import app.core.model.WindPark
import app.core.model.WindTurbine
import app.core.ui.components.formatDataQuality
import app.core.util.formatGermanNumber
import kotlin.math.round

internal const val DEFAULT_FULL_LOAD_HOURS = 2_000.0
internal const val DEFAULT_EMISSION_FACTOR_KG_PER_KWH = 0.38
internal const val DEFAULT_HOUSEHOLD_CONSUMPTION_KWH = 3_500.0
internal const val DEFAULT_MUNICIPAL_BENEFIT_EUR_PER_KWH = 0.002
internal const val CO2_PER_BERLIN_NYC_FLIGHT_KG = 1_000.0
internal const val CO2_PER_CAR_YEAR_KG = 1_500.0
internal const val CO2_PER_COAL_PLANT_YEAR_KG = 1_250_000_000.0
internal const val GERMAN_HOUSEHOLDS_TOTAL = 41_500_000.0

internal data class ComparisonMetrics(
    val turbines: Int,
    val capacityMw: Double,
    val annualProductionKwh: Double,
    val co2Kg: Double,
    val households: Double,
    val municipalBenefit: Double?,
)

internal data class ImpactBarInput(
    val label: String,
    val value: Double,
    val navigateTarget: ImpactNavigateTarget? = null,
)

internal fun List<Metric>.firstValue(metricType: String): Double? =
    firstOrNull { it.metricType == metricType }?.value

internal fun assumptionValue(
    id: String,
    assumptions: List<SnapshotAssumption>,
    fallback: Double,
): Double = assumptions.firstOrNull { it.id == id }?.value ?: fallback

internal fun formatCompact(value: Double): String =
    if (value >= 1_000_000.0) {
        "${formatGermanNumber(value / 1_000_000.0, 1)} Mio."
    } else {
        formatGermanNumber(round(value).toInt())
    }

internal fun formatCurrency(value: Double): String =
    if (value >= 1_000_000_000.0) {
        "${formatGermanNumber(value / 1_000_000_000.0, 1)} Mrd. EUR"
    } else if (value >= 1_000_000.0) {
        "${formatGermanNumber(value / 1_000_000.0, 1)} Mio. EUR"
    } else {
        "${formatGermanNumber(value, 0)} EUR"
    }

internal fun formatCo2(kg: Double): String {
    val mioTons = kg / 1_000_000_000.0
    return "${formatGermanNumber(mioTons, 1)} Mio. t"
}

internal fun Double.roundToInt(): Int = round(this).toInt()

internal fun WindPark.comparisonMetrics(
    metrics: List<Metric>,
    assumptions: List<SnapshotAssumption>,
): ComparisonMetrics {
    val capacityKw = installedCapacityKw?.toDouble() ?: 0.0
    val annualProduction = metrics.firstValue("annual_production")
        ?: capacityKw * assumptionValue("full_load_hours", assumptions, DEFAULT_FULL_LOAD_HOURS)
    val co2 = metrics.firstValue("co2_savings")
        ?: annualProduction * assumptionValue(
            "emission_factor_kg_per_kwh",
            assumptions,
            DEFAULT_EMISSION_FACTOR_KG_PER_KWH,
        )
    val households = metrics.firstValue("household_equivalent")
        ?: metrics.firstValue("households_supplied")
        ?: annualProduction / assumptionValue(
            "household_consumption_kwh",
            assumptions,
            DEFAULT_HOUSEHOLD_CONSUMPTION_KWH,
        )
    val municipalBenefit = metrics.firstValue("municipal_participation")
        ?: annualProduction * assumptionValue(
            "municipal_benefit_eur_per_kwh",
            assumptions,
            DEFAULT_MUNICIPAL_BENEFIT_EUR_PER_KWH,
        )
    return ComparisonMetrics(
        turbines = turbineCount,
        capacityMw = capacityKw / 1_000.0,
        annualProductionKwh = annualProduction,
        co2Kg = co2,
        households = households,
        municipalBenefit = municipalBenefit,
    )
}

internal fun buildHouseholdsImpactDetail(
    parks: List<WindPark>,
    totalHouseholds: Double,
    metricsByParkId: Map<String, List<Metric>>,
    assumptions: List<SnapshotAssumption>,
): HouseholdsImpactDetail {
    val householdsByPark = parks.map { ImpactBarInput(it.name, parkHouseholds(it, metricsByParkId, assumptions), ImpactNavigateTarget.Park(it.id)) }
    val topParks = topBars(householdsByPark, formatter = ::formatCompact)
    val avgPerPark = if (parks.isNotEmpty()) totalHouseholds / parks.size else 0.0
    val nationalShare = totalHouseholds / GERMAN_HOUSEHOLDS_TOTAL
    return HouseholdsImpactDetail(
        summaryValue = formatCompact(totalHouseholds),
        summarySubtitle = "Haushalte mit durchschnittlichem Jahresverbrauch",
        topParks = topParks,
        nationalSharePercent = "${formatGermanNumber(nationalShare * 100.0, 2)} %",
        avgPerPark = formatCompact(avgPerPark),
        assumptions = listOf(
            StatsImpactFact("Annahme", "3.500 kWh/Jahr je Haushalt"),
            StatsImpactFact("Basis", "geschätzte Jahresproduktion"),
            StatsImpactFact("Bezug", "ca. ${formatGermanNumber(GERMAN_HOUSEHOLDS_TOTAL / 1_000_000.0, 1)} Mio. Haushalte in Deutschland"),
            StatsImpactFact("Einordnung", "Orientierungswert, kein Liefervertrag"),
        ),
        qualityLabel = formatDataQuality("estimated"),
    )
}

internal fun buildMunicipalBenefitImpactDetail(
    parks: List<WindPark>,
    districts: List<DistrictStat>,
    states: List<StateStat>,
    metricsByParkId: Map<String, List<Metric>>,
    assumptions: List<SnapshotAssumption>,
): MunicipalBenefitImpactDetail {
    val benefitByPark = parks
        .map { parkMunicipalBenefit(it, metricsByParkId, assumptions) }
        .filter { it > 0.0 }
        .sorted()
    val min = benefitByPark.firstOrNull() ?: 0.0
    val median = benefitByPark.medianOrNull() ?: 0.0
    val max = benefitByPark.lastOrNull() ?: 0.0

    val topDistricts = topBars(
        districts.mapNotNull { d -> d.municipalBenefitEur?.let { ImpactBarInput(d.label, it, ImpactNavigateTarget.Region("district", d.districtId)) } },
        formatter = ::formatCurrency,
    )
    return MunicipalBenefitImpactDetail(
        summaryValue = formatCurrency(states.sumOf { it.municipalBenefitEur ?: 0.0 }),
        summarySubtitle = "pro Jahr nach § 6 EEG orientiert",
        topDistricts = topDistricts,
        minPerPark = formatCurrency(min),
        medianPerPark = formatCurrency(median),
        maxPerPark = formatCurrency(max),
        assumptions = listOf(
            StatsImpactFact("Regel", "§ 6 EEG"),
            StatsImpactFact("Richtwert", "bis zu 0,2 ct/kWh"),
            StatsImpactFact("Gilt für", "Windenergie an Land"),
            StatsImpactFact("Einordnung", "Orientierungswert für mögliche Beteiligung"),
        ),
        qualityLabel = formatDataQuality("estimated"),
    )
}

internal fun buildTurbinesImpactDetail(
    parks: List<WindPark>,
    turbines: List<WindTurbine>,
): TurbinesImpactDetail {
    val byDecade = decadeBuckets(turbines)
    val heightBuckets = heightBuckets(turbines)
    val topParks = topBars(
        parks.map { ImpactBarInput(it.name, it.turbineCount.toDouble(), ImpactNavigateTarget.Park(it.id)) },
        formatter = { formatGermanNumber(it.roundToInt()) },
    )
    val avgPerPark = if (parks.isNotEmpty()) turbines.size.toDouble() / parks.size else 0.0
    return TurbinesImpactDetail(
        summaryValue = formatGermanNumber(turbines.size),
        summarySubtitle = "aus MaStR im lokalen Snapshot",
        byDecade = byDecade,
        heightBuckets = heightBuckets,
        topParks = topParks,
        avgPerPark = "${formatGermanNumber(avgPerPark, 1)} Windanlagen je Windpark",
        assumptions = listOf(
            StatsImpactFact("Quelle", "MaStR"),
            StatsImpactFact("Einheit", "Windanlage"),
            StatsImpactFact("Darstellung", "Windpark als UX-Einheit"),
            StatsImpactFact("Datenstand", "lokaler Snapshot, keine Live-Daten"),
        ),
        qualityLabel = formatDataQuality("official"),
    )
}

internal fun buildCo2ImpactDetail(
    parks: List<WindPark>,
    totalCo2Kg: Double,
    metricsByParkId: Map<String, List<Metric>>,
    assumptions: List<SnapshotAssumption>,
): Co2ImpactDetail {
    val topParks = topBars(
        parks.map { ImpactBarInput(it.name, parkCo2(it, metricsByParkId, assumptions), ImpactNavigateTarget.Park(it.id)) },
        formatter = { "${formatGermanNumber(it / 1_000.0, 0)} t" },
    )
    return Co2ImpactDetail(
        summaryValue = formatCo2(totalCo2Kg),
        summarySubtitle = "pro Jahr gegenüber Strommix-Emissionen",
        topParks = topParks,
        equivalents = buildCo2Comparisons(totalCo2Kg),
        assumptions = listOf(
            StatsImpactFact("Emissionsfaktor", "380 g/kWh"),
            StatsImpactFact("Basis", "geschätzte Jahresproduktion"),
            StatsImpactFact("Einordnung", "transparente Schätzung, keine gemessene Einsparung"),
        ),
        qualityLabel = formatDataQuality("estimated"),
    )
}

internal fun buildCo2Comparisons(totalCo2Kg: Double): List<Co2Comparison> {
    val values = listOf(
        "Flüge Berlin-NYC" to totalCo2Kg / CO2_PER_BERLIN_NYC_FLIGHT_KG,
        "Auto-Jahresfahrten" to totalCo2Kg / CO2_PER_CAR_YEAR_KG,
        "Kohlekraftwerksjahre" to totalCo2Kg / CO2_PER_COAL_PLANT_YEAR_KG,
    )
    return values.map { (label, value) ->
        Co2Comparison(
            label = label,
            value = when (label) {
                "Kohlekraftwerksjahre" -> "ca. ${formatGermanNumber(value, 0)} Jahre"
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

internal fun buildCityStatsFromSummaries(
    citySummaries: List<RegionSummary>,
    stateSummaries: List<RegionSummary>,
    totalCapacityMw: Double,
): List<CityStat> {
    val stateCapacityByName = stateSummaries.associate { it.name to it.installedCapacityKw / 1_000.0 }

    return citySummaries.map { summary ->
        val capacityMw = summary.installedCapacityKw / 1_000.0
        val parentParts = summary.parentName.orEmpty().split(",").map { it.trim() }
        val districtName = parentParts.getOrNull(0).orEmpty()
        val stateName = parentParts.getOrNull(1).orEmpty()
        val stateCapacity = stateCapacityByName[stateName] ?: 0.0
        CityStat(
            cityId = summary.regionId,
            label = summary.name,
            districtName = districtName,
            stateName = stateName,
            windParkCount = summary.windParkCount,
            turbineCount = summary.turbineCount,
            installedCapacityMw = capacityMw,
            shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            shareOfStateCapacity = if (stateCapacity > 0.0) {
                (capacityMw / stateCapacity).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            municipalBenefitEur = summary.municipalBenefitEur,
        )
    }.sortedByDescending { it.installedCapacityMw }
}

internal fun buildDistrictStatsFromSummaries(
    districtSummaries: List<RegionSummary>,
    stateSummaries: List<RegionSummary>,
    totalCapacityMw: Double,
): List<DistrictStat> {
    val stateCapacityByName = stateSummaries.associate { it.name to it.installedCapacityKw / 1_000.0 }

    return districtSummaries.map { summary ->
        val capacityMw = summary.installedCapacityKw / 1_000.0
        val stateName = summary.parentName.orEmpty()
        val stateCapacity = stateCapacityByName[stateName] ?: 0.0
        DistrictStat(
            districtId = summary.regionId,
            label = summary.name,
            contextLabel = summary.contextLabel.orEmpty(),
            stateName = stateName,
            windParkCount = summary.windParkCount,
            turbineCount = summary.turbineCount,
            installedCapacityMw = capacityMw,
            shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            shareOfStateCapacity = if (stateCapacity > 0.0) {
                (capacityMw / stateCapacity).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            municipalBenefitEur = summary.municipalBenefitEur,
        )
    }.sortedByDescending { it.installedCapacityMw }
}

internal fun buildStateStatsFromSummaries(
    stateSummaries: List<RegionSummary>,
    totalCapacityMw: Double,
): List<StateStat> =
    stateSummaries.map { summary ->
        val capacityMw = summary.installedCapacityKw / 1_000.0
        StateStat(
            stateId = summary.regionId,
            label = summary.name,
            windParkCount = summary.windParkCount,
            turbineCount = summary.turbineCount,
            installedCapacityMw = capacityMw,
            shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            municipalBenefitEur = summary.municipalBenefitEur,
        )
    }.sortedByDescending { it.installedCapacityMw }

internal fun buildHouseholdsImpactDetailFromSummary(
    parks: List<WindPark>,
    nationalSummary: NationalStatsSummary?,
    assumptions: List<SnapshotAssumption>,
): HouseholdsImpactDetail {
    val totalHouseholds = nationalSummary?.householdEquivalent ?: 0.0
    val topParks = buildParkImpactBars(
        parks = parks,
        assumptions = assumptions,
        selector = { it.households },
        formatter = ::formatCompact,
    )
    val avgPerPark = if ((nationalSummary?.windParkCount ?: parks.size) > 0) {
        totalHouseholds / (nationalSummary?.windParkCount ?: parks.size)
    } else {
        0.0
    }
    val nationalShare = totalHouseholds / GERMAN_HOUSEHOLDS_TOTAL
    return HouseholdsImpactDetail(
        summaryValue = formatCompact(totalHouseholds),
        summarySubtitle = "Haushalte mit durchschnittlichem Jahresverbrauch",
        topParks = topParks,
        nationalSharePercent = "${formatGermanNumber(nationalShare * 100.0, 2)} %",
        avgPerPark = formatCompact(avgPerPark),
        assumptions = listOf(
            StatsImpactFact("Annahme", "3.500 kWh/Jahr je Haushalt"),
            StatsImpactFact("Basis", "vorberechnete Jahresproduktion"),
            StatsImpactFact("Bezug", "ca. ${formatGermanNumber(GERMAN_HOUSEHOLDS_TOTAL / 1_000_000.0, 1)} Mio. Haushalte in Deutschland"),
            StatsImpactFact("Einordnung", "Orientierungswert, kein Liefervertrag"),
        ),
        qualityLabel = formatDataQuality("estimated"),
    )
}

internal fun buildMunicipalBenefitImpactDetailFromSummary(
    parks: List<WindPark>,
    districts: List<DistrictStat>,
    states: List<StateStat>,
    nationalSummary: NationalStatsSummary?,
    assumptions: List<SnapshotAssumption>,
): MunicipalBenefitImpactDetail {
    val benefitByPark = parks
        .map { it.comparisonMetrics(emptyList(), assumptions).municipalBenefit ?: 0.0 }
        .filter { it > 0.0 }
        .sorted()
    val topDistricts = topBars(
        districts.mapNotNull { district ->
            district.municipalBenefitEur?.let {
                ImpactBarInput(district.label, it, ImpactNavigateTarget.Region("district", district.districtId))
            }
        },
        formatter = ::formatCurrency,
    )
    return MunicipalBenefitImpactDetail(
        summaryValue = formatCurrency(nationalSummary?.municipalBenefitEur ?: states.sumOf { it.municipalBenefitEur ?: 0.0 }),
        summarySubtitle = "pro Jahr nach § 6 EEG orientiert",
        topDistricts = topDistricts,
        minPerPark = formatCurrency(benefitByPark.firstOrNull() ?: 0.0),
        medianPerPark = formatCurrency(benefitByPark.medianOrNull() ?: 0.0),
        maxPerPark = formatCurrency(benefitByPark.lastOrNull() ?: 0.0),
        assumptions = listOf(
            StatsImpactFact("Regel", "§ 6 EEG"),
            StatsImpactFact("Richtwert", "bis zu 0,2 ct/kWh"),
            StatsImpactFact("Gilt für", "Windenergie an Land"),
            StatsImpactFact("Einordnung", "Orientierungswert für mögliche Beteiligung"),
        ),
        qualityLabel = formatDataQuality("estimated"),
    )
}

internal fun buildTurbinesImpactDetailFromSummary(
    parks: List<WindPark>,
    nationalSummary: NationalStatsSummary?,
): TurbinesImpactDetail {
    val totalTurbines = nationalSummary?.activeTurbineCount ?: parks.sumOf { it.turbineCount }
    val byDecade = nationalSummary?.let {
        bucketBars(
            listOf(
                "Vor 2000" to it.turbineCommissioningPre2000.toDouble(),
                "2000-2009" to it.turbineCommissioning2000To2009.toDouble(),
                "2010-2019" to it.turbineCommissioning2010To2019.toDouble(),
                "2020+" to it.turbineCommissioning2020Plus.toDouble(),
                "Unbekannt" to it.turbineCommissioningUnknown.toDouble(),
            )
        )
    } ?: emptyList()
    val heightBuckets = nationalSummary?.let {
        bucketBars(
            listOf(
                "< 80 m" to it.turbineHeightLt80m.toDouble(),
                "80-120 m" to it.turbineHeight80To120m.toDouble(),
                "120-160 m" to it.turbineHeight120To160m.toDouble(),
                "> 160 m" to it.turbineHeightGte160m.toDouble(),
                "Unbekannt" to it.turbineHeightUnknown.toDouble(),
            )
        )
    } ?: emptyList()
    val topParks = topBars(
        parks.map { ImpactBarInput(it.name, it.turbineCount.toDouble(), ImpactNavigateTarget.Park(it.id)) },
        formatter = { formatGermanNumber(it.roundToInt()) },
    )
    val parkCount = (nationalSummary?.windParkCount ?: parks.size).coerceAtLeast(1)
    return TurbinesImpactDetail(
        summaryValue = formatGermanNumber(totalTurbines),
        summarySubtitle = "aus MaStR im lokalen Snapshot",
        byDecade = byDecade,
        heightBuckets = heightBuckets,
        topParks = topParks,
        avgPerPark = "${formatGermanNumber(totalTurbines.toDouble() / parkCount, 1)} Windanlagen je Windpark",
        assumptions = listOf(
            StatsImpactFact("Quelle", "MaStR"),
            StatsImpactFact("Einheit", "Windanlage"),
            StatsImpactFact("Darstellung", "Windpark als UX-Einheit"),
            StatsImpactFact("Datenstand", "lokaler Snapshot, keine Live-Daten"),
        ),
        qualityLabel = formatDataQuality("official"),
    )
}

internal fun buildCo2ImpactDetailFromSummary(
    parks: List<WindPark>,
    nationalSummary: NationalStatsSummary?,
    assumptions: List<SnapshotAssumption>,
): Co2ImpactDetail {
    val totalCo2Kg = nationalSummary?.co2SavingsKg ?: 0.0
    val topParks = buildParkImpactBars(
        parks = parks,
        assumptions = assumptions,
        selector = { it.co2Kg },
        formatter = { "${formatGermanNumber(it / 1_000.0, 0)} t" },
    )
    return Co2ImpactDetail(
        summaryValue = formatCo2(totalCo2Kg),
        summarySubtitle = "pro Jahr gegenüber Strommix-Emissionen",
        topParks = topParks,
        equivalents = buildCo2Comparisons(totalCo2Kg),
        assumptions = listOf(
            StatsImpactFact("Emissionsfaktor", "380 g/kWh"),
            StatsImpactFact("Basis", "vorberechnete Jahresproduktion"),
            StatsImpactFact("Einordnung", "transparente Schätzung, keine gemessene Einsparung"),
        ),
        qualityLabel = formatDataQuality("estimated"),
    )
}

private fun buildParkImpactBars(
    parks: List<WindPark>,
    assumptions: List<SnapshotAssumption>,
    selector: (ComparisonMetrics) -> Double,
    formatter: (Double) -> String,
): List<ImpactBarEntry> =
    topBars(
        parks.map { park ->
            ImpactBarInput(
                label = park.name,
                value = selector(park.comparisonMetrics(emptyList(), assumptions)),
                navigateTarget = ImpactNavigateTarget.Park(park.id),
            )
        },
        formatter = formatter,
    )

private fun parkHouseholds(
    park: WindPark,
    metricsByParkId: Map<String, List<Metric>>,
    assumptions: List<SnapshotAssumption>,
): Double = park.comparisonMetrics(metricsByParkId[park.id] ?: emptyList(), assumptions).households

private fun parkCo2(
    park: WindPark,
    metricsByParkId: Map<String, List<Metric>>,
    assumptions: List<SnapshotAssumption>,
): Double = park.comparisonMetrics(metricsByParkId[park.id] ?: emptyList(), assumptions).co2Kg

private fun parkMunicipalBenefit(
    park: WindPark,
    metricsByParkId: Map<String, List<Metric>>,
    assumptions: List<SnapshotAssumption>,
): Double = park.comparisonMetrics(metricsByParkId[park.id] ?: emptyList(), assumptions).municipalBenefit ?: 0.0

private fun topBars(
    entries: List<ImpactBarInput>,
    formatter: (Double) -> String,
): List<ImpactBarEntry> {
    val max = entries.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
    return entries
        .sortedByDescending { it.value }
        .map { input ->
            ImpactBarEntry(
                label = input.label,
                value = formatter(input.value),
                ratio = (input.value / max).toFloat().coerceIn(0f, 1f),
                navigateTarget = input.navigateTarget,
            )
        }
}

private fun decadeBuckets(turbines: List<WindTurbine>): List<ImpactBarEntry> {
    val buckets = listOf(
        "Vor 2000" to { y: Long -> y < 2000L },
        "2000–2009" to { y: Long -> y in 2000L..2009L },
        "2010–2019" to { y: Long -> y in 2010L..2019L },
        "2020+" to { y: Long -> y >= 2020L },
    )
    val counts = buckets.map { (label, predicate) ->
        label to turbines.count { t -> t.commissioningYear != null && predicate(t.commissioningYear) }.toDouble()
    }
    val unknown = turbines.count { it.commissioningYear == null }.toDouble()
    val all = if (unknown > 0) counts + ("Unbekannt" to unknown) else counts
    return bucketBars(all)
}

private fun heightBuckets(turbines: List<WindTurbine>): List<ImpactBarEntry> {
    val buckets = listOf(
        "< 80 m" to { h: Double -> h < 80.0 },
        "80–120 m" to { h: Double -> h in 80.0..<120.0 },
        "120–160 m" to { h: Double -> h in 120.0..<160.0 },
        "> 160 m" to { h: Double -> h >= 160.0 },
    )
    val counts = buckets.map { (label, predicate) ->
        label to turbines.count { t -> t.hubHeightM != null && predicate(t.hubHeightM) }.toDouble()
    }
    val unknown = turbines.count { it.hubHeightM == null }.toDouble()
    val all = if (unknown > 0) counts + ("Unbekannt" to unknown) else counts
    return bucketBars(all)
}

private fun bucketBars(entries: List<Pair<String, Double>>): List<ImpactBarEntry> {
    val max = entries.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
    return entries.map { (label, value) ->
        ImpactBarEntry(
            label = label,
            value = formatGermanNumber(value.toInt()),
            ratio = (value / max).toFloat().coerceIn(0f, 1f),
        )
    }
}

private fun List<Double>.medianOrNull(): Double? =
    if (isEmpty()) null else {
        val sorted = sorted()
        val mid = size / 2
        if (size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

internal fun buildCityStats(
    parks: List<WindPark>,
    totalCapacityMw: Double,
    metricsByParkId: Map<String, List<Metric>>,
): List<CityStat> {
    val parkGroups = parks.groupBy { it.municipalityId }
    val stateCapacityMw = parks
        .groupBy { it.stateId }
        .mapValues { (_, stateParks) -> stateParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0 }

    return parkGroups.map { (cityId, cityParks) ->
        val capacityMw = cityParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
        val turbines = cityParks.sumOf { it.turbineCount }
        val firstPark = cityParks.first()
        val stateCapacity = stateCapacityMw[firstPark.stateId] ?: 0.0
        val municipalBenefitEur = cityParks
            .takeIf { it.isNotEmpty() }
            ?.sumOf { park ->
                metricsByParkId[park.id]
                    ?.firstOrNull { it.metricType == "municipal_participation" }
                    ?.value
                    ?: 0.0
            }
        CityStat(
            cityId = cityId,
            label = firstPark.municipalityName,
            districtName = firstPark.districtName,
            stateName = firstPark.stateName,
            windParkCount = cityParks.size,
            turbineCount = turbines,
            installedCapacityMw = capacityMw,
            shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            shareOfStateCapacity = if (stateCapacity > 0.0) {
                (capacityMw / stateCapacity).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            municipalBenefitEur = municipalBenefitEur,
        )
    }.sortedByDescending { it.installedCapacityMw }
}

internal fun buildDistrictStats(
    parks: List<WindPark>,
    totalCapacityMw: Double,
    metricsByParkId: Map<String, List<Metric>>,
): List<DistrictStat> {
    val parkGroups = parks.groupBy { it.districtId }
    val stateCapacityMw = parks
        .groupBy { it.stateId }
        .mapValues { (_, stateParks) -> stateParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0 }

    return parkGroups.map { (districtId, districtParks) ->
        val capacityMw = districtParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
        val turbines = districtParks.sumOf { it.turbineCount }
        val firstPark = districtParks.first()
        val representativeMunicipality = districtParks
            .groupBy { it.municipalityName }
            .maxByOrNull { (_, municipalityParks) ->
                municipalityParks.sumOf { it.installedCapacityKw ?: 0L }
            }
            ?.key
            ?: firstPark.municipalityName
        val stateCapacity = stateCapacityMw[firstPark.stateId] ?: 0.0
        val municipalBenefitEur = districtParks
            .takeIf { it.isNotEmpty() }
            ?.sumOf { park ->
                metricsByParkId[park.id]
                    ?.firstOrNull { it.metricType == "municipal_participation" }
                    ?.value
                    ?: 0.0
            }
        DistrictStat(
            districtId = districtId,
            label = firstPark.districtName,
            contextLabel = representativeMunicipality,
            stateName = firstPark.stateName,
            windParkCount = districtParks.size,
            turbineCount = turbines,
            installedCapacityMw = capacityMw,
            shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            shareOfStateCapacity = if (stateCapacity > 0.0) {
                (capacityMw / stateCapacity).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            municipalBenefitEur = municipalBenefitEur,
        )
    }.sortedByDescending { it.installedCapacityMw }
}

internal fun buildStateStats(
    parks: List<WindPark>,
    totalCapacityMw: Double,
    metricsByParkId: Map<String, List<Metric>>,
): List<StateStat> {
    val parkGroups = parks.groupBy { it.stateId }

    return parkGroups.map { (stateId, stateParks) ->
        val capacityMw = stateParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
        val turbines = stateParks.sumOf { it.turbineCount }
        val firstPark = stateParks.first()
        val municipalBenefitEur = stateParks
            .takeIf { it.isNotEmpty() }
            ?.sumOf { park ->
                metricsByParkId[park.id]
                    ?.firstOrNull { it.metricType == "municipal_participation" }
                    ?.value
                    ?: 0.0
            }
        StateStat(
            stateId = stateId,
            label = firstPark.stateName,
            windParkCount = stateParks.size,
            turbineCount = turbines,
            installedCapacityMw = capacityMw,
            shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            municipalBenefitEur = municipalBenefitEur,
        )
    }.sortedByDescending { it.installedCapacityMw }
}
