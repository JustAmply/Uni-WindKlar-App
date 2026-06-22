package app.feature.detail

import app.core.model.WindPark
import app.core.model.SnapshotAssumption

data class RegionDetailUiState(
    val regionId: String,
    val regionType: String, // "city", "district", "state"
    val isLoading: Boolean = true,
    val regionName: String = "",
    val regionTypeLabel: String = "",
    val parentRegionContext: String? = null,
    
    // Parent region relationships for context and hierarchy navigation
    val parentDistrictId: String? = null,
    val parentDistrictName: String? = null,
    val parentStateId: String? = null,
    val parentStateName: String? = null,

    // Summary statistics
    val windParkCount: Int = 0,
    val turbineCount: Int = 0,
    val installedCapacityMw: Double = 0.0,
    val shareOfNationalCapacity: Float = 0f,
    val shareOfStateCapacity: Float? = null,
    val nationalCapacityMw: Double = 0.0,
    val stateCapacityMw: Double? = null,

    // Citizen impact statistics (aggregated for the region)
    val annualProductionGwh: Double = 0.0,
    val co2SavingsTons: Double = 0.0,
    val householdsSupplied: Int = 0,
    val municipalBenefitEur: Double = 0.0,

    val assumptions: List<SnapshotAssumption> = emptyList(),
    val windParks: List<WindPark> = emptyList(),
    val attribution: String = "",
)
