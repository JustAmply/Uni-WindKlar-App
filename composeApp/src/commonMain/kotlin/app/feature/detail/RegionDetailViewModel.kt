package app.feature.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.SnapshotAssumption
import app.core.model.WindPark
import app.core.model.RankingItem
import app.core.model.RankingDetailLine
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class RegionDetailViewModel(
    val regionType: String,
    val regionId: String,
    private val repository: WindParkRepository,
) : ViewModel() {
    var uiState by mutableStateOf(RegionDetailUiState(regionId = regionId, regionType = regionType))
        private set

    init {
        loadRegionDetails()
    }

    private fun loadRegionDetails() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            val allParks = repository.getWindParks()
            val assumptions = repository.getSnapshotAssumptions()
            val attribution = repository.getSnapshotAttribution()

            // Filter parks belonging to this region
            val regionParks = allParks.filter { park ->
                when (regionType.lowercase()) {
                    "city" -> park.municipalityId == regionId
                    "district" -> park.districtId == regionId
                    "state" -> park.stateId == regionId
                    else -> false
                }
            }

            if (regionParks.isEmpty()) {
                uiState = uiState.copy(
                    isLoading = false,
                    regionName = "Unbekannte Region",
                    regionTypeLabel = when (regionType.lowercase()) {
                        "city" -> "Gemeinde"
                        "district" -> "Landkreis"
                        "state" -> "Bundesland"
                        else -> "Region"
                    }
                )
                return@launch
            }

            val firstPark = regionParks.first()
            val regionName = when (regionType.lowercase()) {
                "city" -> firstPark.municipalityName
                "district" -> firstPark.districtName
                "state" -> firstPark.stateName
                else -> ""
            }

            val regionTypeLabel = when (regionType.lowercase()) {
                "city" -> "Gemeinde"
                "district" -> "Landkreis"
                "state" -> "Bundesland"
                else -> "Region"
            }

            val parentRegionContext = when (regionType.lowercase()) {
                "city" -> "${firstPark.districtName}, ${firstPark.stateName}"
                "district" -> firstPark.stateName
                else -> null
            }

            // Parent IDs/names for hierarchy navigation
            val parentDistrictId = if (regionType.lowercase() == "city") firstPark.districtId else null
            val parentDistrictName = if (regionType.lowercase() == "city") firstPark.districtName else null
            val parentStateId = when (regionType.lowercase()) {
                "city" -> firstPark.stateId
                "district" -> firstPark.stateId
                else -> null
            }
            val parentStateName = when (regionType.lowercase()) {
                "city" -> firstPark.stateName
                "district" -> firstPark.stateName
                else -> null
            }

            val windParkCount = regionParks.size
            val turbineCount = regionParks.sumOf { it.turbineCount }
            val installedCapacityKw = regionParks.sumOf { it.installedCapacityKw ?: 0L }
            val installedCapacityMw = installedCapacityKw / 1000.0

            val totalCapacityKw = allParks.sumOf { it.installedCapacityKw ?: 0L }
            val totalCapacityMw = totalCapacityKw / 1000.0
            val shareOfNationalCapacity = if (totalCapacityKw > 0) (installedCapacityKw.toDouble() / totalCapacityKw).toFloat() else 0f

            val (shareOfStateCapacity, stateCapacityMw) = if (regionType.lowercase() == "city" || regionType.lowercase() == "district") {
                val stateParks = allParks.filter { it.stateId == firstPark.stateId }
                val stateCapKw = stateParks.sumOf { it.installedCapacityKw ?: 0L }
                val stateCapMw = stateCapKw / 1000.0
                val share = if (stateCapKw > 0) (installedCapacityKw.toDouble() / stateCapKw).toFloat() else 0f
                Pair(share, stateCapMw)
            } else {
                Pair(null, null)
            }

            // Calculations based on assumptions (matching StatsViewModel defaults if missing)
            val fullLoadHours = assumptions.firstOrNull { it.id == "full_load_hours" }?.value ?: 2000.0
            val emissionFactor = assumptions.firstOrNull { it.id == "emission_factor_kg_per_kwh" }?.value ?: 0.38
            val householdCons = assumptions.firstOrNull { it.id == "household_consumption_kwh" }?.value ?: 3500.0
            val municipalBenefitFactor = assumptions.firstOrNull { it.id == "municipal_benefit_eur_per_kwh" }?.value ?: 0.002

            val annualProductionKwh = installedCapacityKw.toDouble() * fullLoadHours
            val annualProductionGwh = annualProductionKwh / 1_000_000.0
            val co2SavingsTons = (annualProductionKwh * emissionFactor) / 1000.0
            val householdsSupplied = (annualProductionKwh / householdCons).toInt()
            val municipalBenefitEur = annualProductionKwh * municipalBenefitFactor

            // Sub-region rankings logic
            val subRegionRankings = when (regionType.lowercase()) {
                "state" -> {
                    val groupedDistricts = regionParks.groupBy { it.districtId }
                    val districtStats = groupedDistricts.map { (distId, parks) ->
                        val capKw = parks.sumOf { it.installedCapacityKw ?: 0L }
                        val turbines = parks.sumOf { it.turbineCount }
                        val name = parks.first().districtName
                        Triple(distId, name, Triple(parks.size, turbines, capKw))
                    }.sortedByDescending { it.third.third }

                    val maxCap = districtStats.firstOrNull()?.third?.third?.toDouble()?.coerceAtLeast(1.0) ?: 1.0
                    districtStats.mapIndexed { index, (distId, name, stats) ->
                        val (parksCount, turbinesCount, capKw) = stats
                        val capMw = capKw / 1000.0
                        val share = if (installedCapacityKw > 0) (capKw.toDouble() / installedCapacityKw).toFloat() else 0f
                        RankingItem(
                            id = distId,
                            rank = index + 1,
                            name = name,
                            subtitle = regionName,
                            valueLabel = "${capMw.roundTo(1).toString().replace(".", ",")} MW",
                            progress = (capKw.toDouble() / maxCap).toFloat().coerceIn(0f, 1f),
                            details = listOf(
                                RankingDetailLine("Windparks", parksCount.toString()),
                                RankingDetailLine("Anlagen", turbinesCount.toString()),
                                RankingDetailLine("Anteil am Bundesland", formatPercent(share)),
                            )
                        )
                    }
                }
                "district" -> {
                    val groupedCities = regionParks.groupBy { it.municipalityId }
                    val cityStats = groupedCities.map { (cityId, parks) ->
                        val capKw = parks.sumOf { it.installedCapacityKw ?: 0L }
                        val turbines = parks.sumOf { it.turbineCount }
                        val name = parks.first().municipalityName
                        Triple(cityId, name, Triple(parks.size, turbines, capKw))
                    }.sortedByDescending { it.third.third }

                    val maxCap = cityStats.firstOrNull()?.third?.third?.toDouble()?.coerceAtLeast(1.0) ?: 1.0
                    cityStats.mapIndexed { index, (cityId, name, stats) ->
                        val (parksCount, turbinesCount, capKw) = stats
                        val capMw = capKw / 1000.0
                        val share = if (installedCapacityKw > 0) (capKw.toDouble() / installedCapacityKw).toFloat() else 0f
                        RankingItem(
                            id = cityId,
                            rank = index + 1,
                            name = name,
                            subtitle = regionName,
                            valueLabel = "${capMw.roundTo(1).toString().replace(".", ",")} MW",
                            progress = (capKw.toDouble() / maxCap).toFloat().coerceIn(0f, 1f),
                            details = listOf(
                                RankingDetailLine("Windparks", parksCount.toString()),
                                RankingDetailLine("Anlagen", turbinesCount.toString()),
                                RankingDetailLine("Anteil am Landkreis", formatPercent(share)),
                            )
                        )
                    }
                }
                else -> emptyList()
            }

            uiState = RegionDetailUiState(
                regionId = regionId,
                regionType = regionType,
                isLoading = false,
                regionName = regionName,
                regionTypeLabel = regionTypeLabel,
                parentRegionContext = parentRegionContext,
                parentDistrictId = parentDistrictId,
                parentDistrictName = parentDistrictName,
                parentStateId = parentStateId,
                parentStateName = parentStateName,
                windParkCount = windParkCount,
                turbineCount = turbineCount,
                installedCapacityMw = installedCapacityMw,
                shareOfNationalCapacity = shareOfNationalCapacity,
                shareOfStateCapacity = shareOfStateCapacity,
                nationalCapacityMw = totalCapacityMw,
                stateCapacityMw = stateCapacityMw,
                annualProductionGwh = annualProductionGwh,
                co2SavingsTons = co2SavingsTons,
                householdsSupplied = householdsSupplied,
                municipalBenefitEur = municipalBenefitEur,
                assumptions = assumptions,
                windParks = regionParks.sortedBy { it.name },
                subRegionRankings = subRegionRankings,
                attribution = attribution,
            )
        }
    }
}

private fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

private fun formatPercent(value: Float): String {
    val pct = value * 100
    val rounded = (pct * 10).roundToInt() / 10.0
    return rounded.toString().replace(".", ",") + "%"
}
