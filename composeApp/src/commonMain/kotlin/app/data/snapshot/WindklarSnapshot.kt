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
    val latitude: Double,
    val longitude: Double,
    val installedCapacityKw: Long? = null,
    val status: String? = null,
    val turbineType: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val hubHeightM: Double? = null,
    val rotorDiameterM: Double? = null,
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
