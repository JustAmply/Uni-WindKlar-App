package app.feature.stats

data class StatsUiState(
    val metrics: List<StatsMetric> = listOf(
        StatsMetric(
            value = "1.250",
            label = "Windparks",
            icon = StatsMetricIcon.Wind,
        ),
        StatsMetric(
            value = "615 TWh",
            label = "Produktion",
            icon = StatsMetricIcon.Production,
        ),
        StatsMetric(
            value = "42%",
            label = "Erneuerbar",
            icon = StatsMetricIcon.Renewable,
        ),
    ),
    val growthPercentage: String = "+18%",
    val annualProduction: List<AnnualProductionPoint> = listOf(
        AnnualProductionPoint("2020", 250f),
        AnnualProductionPoint("2021", 310f),
        AnnualProductionPoint("2022", 380f),
        AnnualProductionPoint("2023", 450f),
        AnnualProductionPoint("2024", 520f),
        AnnualProductionPoint("2025", 615f),
    ),
    val co2Reduction: Co2Reduction = Co2Reduction(
        value = "18,5 Mio. t",
        label = "CO2 Einsparung 2025",
        equivalent = "Entspricht 92,5 Millionen gepflanzten Baeumen",
    ),
    val energyMix: List<EnergyMixValue> = listOf(
        EnergyMixValue("Wind", 42f),
        EnergyMixValue("Solar", 28f),
        EnergyMixValue("Andere", 30f),
    ),
)

data class StatsMetric(
    val value: String,
    val label: String,
    val icon: StatsMetricIcon,
)

enum class StatsMetricIcon {
    Wind,
    Production,
    Renewable,
}

data class AnnualProductionPoint(
    val year: String,
    val value: Float,
)

data class Co2Reduction(
    val value: String,
    val label: String,
    val equivalent: String,
)

data class EnergyMixValue(
    val label: String,
    val percentage: Float,
)
