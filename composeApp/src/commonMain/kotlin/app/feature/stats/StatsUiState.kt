package app.feature.stats

import app.core.model.RankingItem
import app.core.model.RankingDetailLine

data class StatsUiState(
    val subtitle: String = "Snapshot wird geladen",
    val snapshotInfoLine: String = "",
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
    val householdsDetail: HouseholdsImpactDetail? = null,
    val municipalBenefitDetail: MunicipalBenefitImpactDetail? = null,
    val turbinesDetail: TurbinesImpactDetail? = null,
    val co2Detail: Co2ImpactDetail? = null,
    val qualityNotes: List<StatsQualityNote> = emptyList(),
    val attribution: String = "",
    val isLoading: Boolean = true,
    val selectedTab: StatsTab = StatsTab.OVERVIEW,
)

enum class StatsTab {
    OVERVIEW,
    RANKINGS,
    COMPARISON
}

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
    val type: StatsImpactType,
    val title: String,
    val value: String,
    val description: String,
    val quality: String,
    val metaLabel: String,
    val icon: StatsIcon,
)

enum class StatsImpactType {
    Households,
    MunicipalBenefit,
    Turbines,
    Co2,
}

data class StatsImpactFact(
    val label: String,
    val value: String,
)

data class ImpactBarEntry(
    val label: String,
    val value: String,
    val ratio: Float,
    val navigateTarget: ImpactNavigateTarget? = null,
)

sealed interface ImpactNavigateTarget {
    data class Park(val id: String) : ImpactNavigateTarget
    data class Region(val type: String, val id: String) : ImpactNavigateTarget
}

data class HouseholdsImpactDetail(
    val summaryValue: String,
    val summarySubtitle: String,
    val topParks: List<ImpactBarEntry>,
    val nationalSharePercent: String,
    val avgPerPark: String,
    val assumptions: List<StatsImpactFact>,
    val qualityLabel: String,
)

data class MunicipalBenefitImpactDetail(
    val summaryValue: String,
    val summarySubtitle: String,
    val topDistricts: List<ImpactBarEntry>,
    val minPerPark: String,
    val medianPerPark: String,
    val maxPerPark: String,
    val assumptions: List<StatsImpactFact>,
    val qualityLabel: String,
)

data class TurbinesImpactDetail(
    val summaryValue: String,
    val summarySubtitle: String,
    val byDecade: List<ImpactBarEntry>,
    val heightBuckets: List<ImpactBarEntry>,
    val topParks: List<ImpactBarEntry>,
    val avgPerPark: String,
    val assumptions: List<StatsImpactFact>,
    val qualityLabel: String,
)

data class Co2ImpactDetail(
    val summaryValue: String,
    val summarySubtitle: String,
    val topParks: List<ImpactBarEntry>,
    val equivalents: List<Co2Comparison>,
    val assumptions: List<StatsImpactFact>,
    val qualityLabel: String,
)

data class ImpactDetailUiState(
    val metricType: String,
    val isLoading: Boolean = true,
    val householdsDetail: HouseholdsImpactDetail? = null,
    val municipalBenefitDetail: MunicipalBenefitImpactDetail? = null,
    val turbinesDetail: TurbinesImpactDetail? = null,
    val co2Detail: Co2ImpactDetail? = null,
)

fun StatsUiState.toImpactDetailUiState(metricType: String): ImpactDetailUiState =
    ImpactDetailUiState(
        metricType = metricType,
        isLoading = isLoading,
        householdsDetail = householdsDetail.takeIf { metricType == StatsImpactType.Households.name },
        municipalBenefitDetail = municipalBenefitDetail.takeIf { metricType == StatsImpactType.MunicipalBenefit.name },
        turbinesDetail = turbinesDetail.takeIf { metricType == StatsImpactType.Turbines.name },
        co2Detail = co2Detail.takeIf { metricType == StatsImpactType.Co2.name },
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
    val municipalBenefitEur: Double? = null,
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
    val municipalBenefitEur: Double? = null,
)

data class StateStat(
    val stateId: String,
    val label: String,
    val windParkCount: Int,
    val turbineCount: Int,
    val installedCapacityMw: Double,
    val shareOfNationalCapacity: Float,
    val municipalBenefitEur: Double? = null,
)

data class DistrictComparison(
    val districtId: String,
    val label: String,
    val contextLabel: String,
    val rankText: String,
    val installedCapacity: String,
    val windParks: String,
    val turbines: String,
    val nationalShare: String,
    val shareProgress: Float,
)

data class Co2Comparison(
    val label: String,
    val value: String,
    val description: String,
)

data class CapacityClassStat(
    val label: String,
    val description: String,
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
