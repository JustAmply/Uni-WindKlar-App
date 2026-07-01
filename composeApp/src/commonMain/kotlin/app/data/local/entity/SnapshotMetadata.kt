package app.data.local.entity

data class SnapshotMetadata(
    val snapshotId: String,
    val sourceName: String,
    val attribution: String,
    val mastrExportDate: String,
    val processedAt: String,
    val pipelineVersion: String,
    val limitations: String,
    val assumptionsJson: String,
)
