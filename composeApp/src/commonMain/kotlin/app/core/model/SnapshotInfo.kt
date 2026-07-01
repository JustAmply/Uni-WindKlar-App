package app.core.model

data class SnapshotInfo(
    val snapshotId: String,
    val sourceName: String,
    val attribution: String,
    val mastrExportDate: String,
    val processedAt: String,
    val pipelineVersion: String,
    val limitations: List<String>,
    val isLocalSnapshot: Boolean = true,
)
