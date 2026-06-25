package app.data.snapshot

import kotlinx.serialization.Serializable

@Serializable
data class WindklarSnapshot(
    val schemaVersion: String,
    val snapshotMetadata: SnapshotMetadataDto,
    val assumptions: List<SnapshotAssumptionDto>,
    val windTurbines: List<WindTurbineDto>,
    val windParks: List<WindParkDto>,
    val metrics: List<MetricDto>,
    val parkOperationalSummaries: List<ParkOperationalSummaryDto> = emptyList(),
    val regionSummaries: List<RegionSummaryDto> = emptyList(),
    val mapSearchEntries: List<MapSearchEntryDto> = emptyList(),
    val nationalStatsSummary: NationalStatsSummaryDto? = null,
)

@Serializable
data class SnapshotMetadataDto(
    val snapshotId: String,
    val sourceName: String,
    val sourceUrl: String,
    val attribution: String,
    val mastrExportDate: String,
    val processedAt: String,
    val pipelineVersion: String,
    val checksumSha256: String,
    val limitations: List<String>,
)

@Serializable
data class SnapshotAssumptionDto(
    val id: String,
    val label: String,
    val value: Double,
    val unit: String,
    val sourceName: String,
    val sourceUrl: String,
    val sourceDate: String,
    val calculationNote: String,
)

@Serializable
data class WindTurbineDto(
    val id: String,
    val windParkId: String,
    val name: String,
    val municipalityId: String,
    val municipalityName: String,
    val districtId: String,
    val districtName: String,
    val stateId: String,
    val stateName: String,
    val latitude: Double,
    val longitude: Double,
    val installedCapacityKw: Long? = null,
    val status: String? = null,
    val turbineType: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val hubHeightM: Double? = null,
    val rotorDiameterM: Double? = null,
    val commissioningYear: Int? = null,
    val sourceName: String,
    val sourceUrl: String,
    val sourceUpdatedAt: String,
    val dataQuality: String,
)

@Serializable
data class WindParkDto(
    val id: String,
    val name: String,
    val municipalityId: String,
    val municipalityName: String,
    val districtId: String,
    val districtName: String,
    val stateId: String,
    val stateName: String,
    val latitude: Double,
    val longitude: Double,
    val turbineCount: Long,
    val installedCapacityKw: Long? = null,
    val turbineIds: List<String>,
    val groupingMethod: String,
    val sourceName: String,
    val sourceUrl: String,
    val sourceUpdatedAt: String,
    val dataQuality: String,
)

@Serializable
data class MetricDto(
    val id: String,
    val subjectType: String,
    val subjectId: String,
    val metricType: String,
    val value: Double? = null,
    val unit: String,
    val period: String? = null,
    val sourceName: String,
    val sourceUrl: String,
    val sourceUpdatedAt: String,
    val dataQuality: String,
    val calculationNote: String? = null,
)

@Serializable
data class ParkOperationalSummaryDto(
    val windParkId: String,
    val parkStatus: String,
    val validTurbineCount: Long,
    val validCapacityKw: Long,
)

@Serializable
data class RegionSummaryDto(
    val regionType: String,
    val regionId: String,
    val name: String,
    val contextLabel: String? = null,
    val parentName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val windParkCount: Long,
    val turbineCount: Long,
    val installedCapacityKw: Long,
    val annualProductionKwh: Double,
    val co2SavingsKg: Double,
    val householdEquivalent: Double,
    val municipalBenefitEur: Double,
)

@Serializable
data class MapSearchEntryDto(
    val id: String,
    val resultType: String,
    val targetId: String,
    val label: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val typeRank: Long,
    val haystack: String,
    val sortName: String,
)

@Serializable
data class NationalStatsSummaryDto(
    val windParkCount: Long,
    val activeTurbineCount: Long,
    val installedCapacityKw: Long,
    val annualProductionKwh: Double,
    val co2SavingsKg: Double,
    val householdEquivalent: Double,
    val municipalBenefitEur: Double,
    val capacityClassLt5Mw: Long,
    val capacityClass5To20Mw: Long,
    val capacityClass20To50Mw: Long,
    val capacityClassGte50Mw: Long,
    val turbineCommissioningPre2000: Long,
    val turbineCommissioning2000To2009: Long,
    val turbineCommissioning2010To2019: Long,
    val turbineCommissioning2020Plus: Long,
    val turbineCommissioningUnknown: Long,
    val turbineHeightLt80m: Long,
    val turbineHeight80To120m: Long,
    val turbineHeight120To160m: Long,
    val turbineHeightGte160m: Long,
    val turbineHeightUnknown: Long,
)
