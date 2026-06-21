package app.feature.stats

data class StatsUiState(
    val subtitle: String = "Snapshot wird geladen",
    val overviewCards: List<StatsOverviewCard> = emptyList(),
    val impactCards: List<StatsImpactCard> = emptyList(),
    val topDistricts: List<DistrictStat> = emptyList(),
    val districtComparison: DistrictComparison? = null,
    val co2Summary: String = "",
    val co2Comparisons: List<Co2Comparison> = emptyList(),
    val capacityClasses: List<CapacityClassStat> = emptyList(),
    val qualityNotes: List<StatsQualityNote> = emptyList(),
    val attribution: String = "",
    val isLoading: Boolean = true,
)

data class StatsOverviewCard(
    val value: String,
    val label: String,
    val icon: StatsIcon,
)

data class StatsImpactCard(
    val title: String,
    val value: String,
    val description: String,
    val quality: String,
    val icon: StatsIcon,
)

data class DistrictStat(
    val districtId: String,
    val label: String,
    val contextLabel: String,
    val windParkCount: Int,
    val turbineCount: Int,
    val installedCapacityMw: Double,
    val shareOfNationalCapacity: Float,
)

data class DistrictComparison(
    val label: String,
    val contextLabel: String,
    val rankText: String,
    val installedCapacity: String,
    val windParks: String,
    val turbines: String,
    val nationalShare: String,
    val shareProgress: Float,
    val isFallback: Boolean,
)

data class Co2Comparison(
    val label: String,
    val value: String,
    val description: String,
)

data class CapacityClassStat(
    val label: String,
    val count: Int,
    val share: Float,
)

data class StatsQualityNote(
    val label: String,
    val quality: String,
    val description: String,
)

enum class StatsIcon {
    Wind,
    Production,
    Capacity,
    Household,
    Co2,
    Money,
    District,
    DataQuality,
}
