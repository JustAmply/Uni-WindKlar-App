package app.core.model

data class ParkOperationalSummary(
    val windParkId: String,
    val parkStatus: String,
    val validTurbineCount: Int,
    val validCapacityKw: Long,
)

data class RegionSummary(
    val regionType: String,
    val regionId: String,
    val name: String,
    val contextLabel: String?,
    val parentName: String?,
    val latitude: Double,
    val longitude: Double,
    val windParkCount: Int,
    val turbineCount: Int,
    val installedCapacityKw: Long,
    val annualProductionKwh: Double,
    val co2SavingsKg: Double,
    val householdEquivalent: Double,
    val municipalBenefitEur: Double,
)

data class MapSearchEntry(
    val id: String,
    val resultType: String,
    val targetId: String,
    val label: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val typeRank: Int,
    val haystack: String,
    val sortName: String,
)

data class NationalStatsSummary(
    val windParkCount: Int,
    val activeTurbineCount: Int,
    val installedCapacityKw: Long,
    val annualProductionKwh: Double,
    val co2SavingsKg: Double,
    val householdEquivalent: Double,
    val municipalBenefitEur: Double,
    val capacityClassLt5Mw: Int,
    val capacityClass5To20Mw: Int,
    val capacityClass20To50Mw: Int,
    val capacityClassGte50Mw: Int,
    val turbineCommissioningPre2000: Int,
    val turbineCommissioning2000To2009: Int,
    val turbineCommissioning2010To2019: Int,
    val turbineCommissioning2020Plus: Int,
    val turbineCommissioningUnknown: Int,
    val turbineHeightLt80m: Int,
    val turbineHeight80To120m: Int,
    val turbineHeight120To160m: Int,
    val turbineHeightGte160m: Int,
    val turbineHeightUnknown: Int,
)
