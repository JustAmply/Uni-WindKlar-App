package app.feature.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.SnapshotAssumption
import app.core.model.WindPark
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch

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
                attribution = attribution,
            )
        }
    }
}
