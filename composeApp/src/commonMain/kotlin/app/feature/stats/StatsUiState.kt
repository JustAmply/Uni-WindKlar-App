package app.feature.stats

import app.core.model.RankingItem
import app.core.model.RankingDetailLine

data class StatsUiState(
    val subtitle: String = "Snapshot wird geladen",
    val overviewCards: List<StatsOverviewCard> = emptyList(),
    val impactCards: List<StatsImpactCard> = emptyList(),
    val rankingType: RankingType = RankingType.DISTRICTS,
    val rankingItems: List<RankingItem> = emptyList(),
    val districtComparison: DistrictComparison? = null,
    val comparisonType: ComparisonType = ComparisonType.PARKS,
    val allParks: List<ComparisonOption> = emptyList(),
    val allCities: List<ComparisonOption> = emptyList(),
    val allDistricts: List<ComparisonOption> = emptyList(),
    val allStates: List<ComparisonOption> = emptyList(),
    val selectedParkA: ComparisonOption? = null,
    val selectedParkB: ComparisonOption? = null,
    val selectedCityA: ComparisonOption? = null,
    val selectedCityB: ComparisonOption? = null,
    val selectedDistrictA: ComparisonOption? = null,
    val selectedDistrictB: ComparisonOption? = null,
    val selectedStateA: ComparisonOption? = null,
    val selectedStateB: ComparisonOption? = null,
    val comparisonRows: List<ComparisonRow> = emptyList(),
    val co2Summary: String = "",
    val co2Comparisons: List<Co2Comparison> = emptyList(),
    val capacityClasses: List<CapacityClassStat> = emptyList(),
    val qualityNotes: List<StatsQualityNote> = emptyList(),
    val attribution: String = "",
    val isLoading: Boolean = true,
)

enum class ComparisonType {
    PARKS,
    CITIES,
    DISTRICTS,
    STATES,
}

enum class RankingType {
    PARKS,
    CITIES,
    DISTRICTS,
    STATES,
}


data class ComparisonOption(
    val id: String,
    val label: String,
    val description: String,
)

data class ComparisonRow(
    val label: String,
    val valueA: String,
    val valueB: String,
    val ratioA: Float,
    val ratioB: Float,
    val isHigherA: Boolean,
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

data class CityStat(
    val cityId: String,
    val label: String,
    val districtName: String,
    val stateName: String,
    val windParkCount: Int,
    val turbineCount: Int,
    val installedCapacityMw: Double,
    val shareOfNationalCapacity: Float,
    val shareOfStateCapacity: Float,
)

data class DistrictStat(
    val districtId: String,
    val label: String,
    val contextLabel: String,
    val stateName: String,
    val windParkCount: Int,
    val turbineCount: Int,
    val installedCapacityMw: Double,
    val shareOfNationalCapacity: Float,
    val shareOfStateCapacity: Float,
)

data class StateStat(
    val stateId: String,
    val label: String,
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
    val percentOfTotal: Float,
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
