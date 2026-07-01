package app.data.local.dao

import app.data.local.source.SourceDatabase
import app.data.local.entity.SnapshotMetadata

interface SnapshotMetadataDao {
    suspend fun getLatest(): SnapshotMetadata?
}

class SqlDelightSnapshotMetadataDao(private val database: SourceDatabase) : SnapshotMetadataDao {
    override suspend fun getLatest(): SnapshotMetadata? {
        return database.snapshotMetadataQueries.selectLatestSnapshot().executeAsOneOrNull()?.let {
            SnapshotMetadata(
                snapshotId = it.snapshot_id,
                sourceName = it.source_name,
                attribution = it.attribution,
                mastrExportDate = it.mastr_export_date,
                processedAt = it.processed_at,
                pipelineVersion = it.pipeline_version,
                limitations = it.limitations,
                assumptionsJson = it.assumptions_json
            )
        }
    }
}
