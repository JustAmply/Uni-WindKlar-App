package app.data.seed

import app.data.local.db.AppDatabase
import app.data.snapshot.SnapshotProvider
import app.data.snapshot.WindklarSnapshot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SnapshotSeedDataImporter(
    private val database: AppDatabase,
    private val snapshotProvider: SnapshotProvider,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : SeedDataImporter {
    override suspend fun importIfNeeded() {
        val snapshot = json.decodeFromString<WindklarSnapshot>(
            snapshotProvider.readSnapshotJson(),
        )
        val metadata = snapshot.snapshotMetadata
        val existingSnapshot = database.snapshotMetadataQueries
            .selectSnapshotByChecksum(metadata.checksumSha256)
            .executeAsOneOrNull()

        if (existingSnapshot != null) return

        database.transaction {
            snapshot.windParks.forEach { park ->
                database.windParkQueries.upsertWindPark(
                    id = park.id,
                    name = park.name,
                    municipality_id = park.municipalityId,
                    municipality_name = park.municipalityName,
                    latitude = park.latitude,
                    longitude = park.longitude,
                    turbine_count = park.turbineCount,
                    installed_capacity_kw = park.installedCapacityKw,
                    grouping_method = park.groupingMethod,
                    source_name = park.sourceName,
                    source_url = park.sourceUrl,
                    source_updated_at = park.sourceUpdatedAt,
                    data_quality = park.dataQuality,
                )
            }

            snapshot.windTurbines.forEach { turbine ->
                database.windTurbineQueries.upsertWindTurbine(
                    id = turbine.id,
                    wind_park_id = turbine.windParkId,
                    name = turbine.name,
                    municipality_id = turbine.municipalityId,
                    municipality_name = turbine.municipalityName,
                    latitude = turbine.latitude,
                    longitude = turbine.longitude,
                    installed_capacity_kw = turbine.installedCapacityKw,
                    status = turbine.status,
                    turbine_type = turbine.turbineType,
                    manufacturer = turbine.manufacturer,
                    model = turbine.model,
                    hub_height_m = turbine.hubHeightM,
                    rotor_diameter_m = turbine.rotorDiameterM,
                    source_name = turbine.sourceName,
                    source_url = turbine.sourceUrl,
                    source_updated_at = turbine.sourceUpdatedAt,
                    data_quality = turbine.dataQuality,
                )
            }

            snapshot.metrics.forEach { metric ->
                database.metricQueries.upsertMetric(
                    id = metric.id,
                    subject_type = metric.subjectType,
                    subject_id = metric.subjectId,
                    metric_type = metric.metricType,
                    metric_value = metric.value,
                    unit = metric.unit,
                    period = metric.period,
                    source_name = metric.sourceName,
                    source_url = metric.sourceUrl,
                    source_updated_at = metric.sourceUpdatedAt,
                    data_quality = metric.dataQuality,
                    calculation_note = metric.calculationNote,
                )
            }

            database.snapshotMetadataQueries.upsertSnapshotMetadata(
                snapshot_id = metadata.snapshotId,
                schema_version = snapshot.schemaVersion,
                source_name = metadata.sourceName,
                source_url = metadata.sourceUrl,
                attribution = metadata.attribution,
                mastr_export_date = metadata.mastrExportDate,
                processed_at = metadata.processedAt,
                pipeline_version = metadata.pipelineVersion,
                checksum_sha256 = metadata.checksumSha256,
                assumptions_json = json.encodeToString(snapshot.assumptions),
                limitations = metadata.limitations.joinToString(separator = "\n"),
                imported_at = metadata.processedAt,
            )
        }
    }
}
