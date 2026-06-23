package app.data.seed

import app.data.local.db.AppDatabase
import app.data.snapshot.SnapshotProvider
import app.data.snapshot.WindklarSnapshot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SnapshotSeedDataImporter(
    private val database: AppDatabase,
    private val snapshotProvider: SnapshotProvider,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : SeedDataImporter {
    private companion object {
        const val SNAPSHOT_METRIC_IMPORT_VERSION_KEY = "snapshot_metric_import_version"
        const val SNAPSHOT_METRIC_IMPORT_VERSION = "snapshot_metrics_v1"
    }

    override suspend fun importIfNeeded(): Unit = withContext(Dispatchers.Default) {
        println("SnapshotSeedDataImporter: Starting importIfNeeded...")
        try {
            // Fast-path checksum detection using the 1 KB metadata file to avoid loading 64 MB of JSON on every launch
            val metadataJson = snapshotProvider.readMetadataJson()
            println("SnapshotSeedDataImporter: Read metadata snapshot. Size = ${metadataJson.length} characters.")
            
            var fastPathChecksum: String? = null
            val checkIndex = metadataJson.indexOf("\"checksumSha256\"")
            if (checkIndex != -1) {
                val colonIndex = metadataJson.indexOf(":", checkIndex)
                if (colonIndex != -1) {
                    val firstQuote = metadataJson.indexOf("\"", colonIndex)
                    if (firstQuote != -1) {
                        val secondQuote = metadataJson.indexOf("\"", firstQuote + 1)
                        if (secondQuote != -1 && secondQuote - firstQuote == 65) {
                            fastPathChecksum = metadataJson.substring(firstQuote + 1, secondQuote)
                        }
                    }
                }
            }

            if (fastPathChecksum != null) {
                println("SnapshotSeedDataImporter: Found fast-path checksum: $fastPathChecksum")
                val existingSnapshot = database.snapshotMetadataQueries
                    .selectSnapshotByChecksum(fastPathChecksum)
                    .executeAsOneOrNull()
                val hasCurrentMetricImport = database.settingQueries
                    .getSetting(SNAPSHOT_METRIC_IMPORT_VERSION_KEY)
                    .executeAsOneOrNull() == SNAPSHOT_METRIC_IMPORT_VERSION
                if (existingSnapshot != null && hasCurrentMetricImport) {
                    println("SnapshotSeedDataImporter: Fast-path checksum matches database. Skipping full JSON parsing and import.")
                    return@withContext
                }
                if (existingSnapshot != null) {
                    println("SnapshotSeedDataImporter: Checksum exists, but metric import version is outdated. Re-importing snapshot metrics.")
                }
            }

            println("SnapshotSeedDataImporter: Seeding database required. Reading full JSON snapshot...")
            val jsonString = snapshotProvider.readSnapshotJson()
            println("SnapshotSeedDataImporter: Read JSON snapshot. Size = ${jsonString.length} characters.")

            println("SnapshotSeedDataImporter: Decoding full JSON snapshot...")
            val snapshot = json.decodeFromString<WindklarSnapshot>(jsonString)
            println("SnapshotSeedDataImporter: Decoded JSON. Parks = ${snapshot.windParks.size}, Turbines = ${snapshot.windTurbines.size}, Metrics = ${snapshot.metrics.size}")
            require(snapshot.metrics.isNotEmpty()) {
                "Snapshot contains no metrics. WindKlar requires app-ready Metric values in the bundled snapshot; runtime metric generation is not supported."
            }
            
            val metadata = snapshot.snapshotMetadata
            println("SnapshotSeedDataImporter: Checking if snapshot already exists in DB (checksum: ${metadata.checksumSha256})...")
            val existingSnapshot = database.snapshotMetadataQueries
                .selectSnapshotByChecksum(metadata.checksumSha256)
                .executeAsOneOrNull()
            val hasCurrentMetricImport = database.settingQueries
                .getSetting(SNAPSHOT_METRIC_IMPORT_VERSION_KEY)
                .executeAsOneOrNull() == SNAPSHOT_METRIC_IMPORT_VERSION

            if (existingSnapshot != null && hasCurrentMetricImport) {
                println("SnapshotSeedDataImporter: Snapshot checksum already exists in DB. Skipping import.")
                return@withContext
            }
            if (existingSnapshot != null) {
                println("SnapshotSeedDataImporter: Snapshot checksum exists, but metric import version is outdated. Re-importing snapshot data.")
            }

            println("SnapshotSeedDataImporter: Seeding database within transaction...")
            database.transaction {
                println("SnapshotSeedDataImporter: Seeding wind parks...")
                snapshot.windParks.forEach { park ->
                    database.windParkQueries.upsertWindPark(
                        id = park.id,
                        name = park.name,
                        municipality_id = park.municipalityId,
                        municipality_name = park.municipalityName,
                        district_id = park.districtId,
                        district_name = park.districtName,
                        state_id = park.stateId,
                        state_name = park.stateName,
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

                println("SnapshotSeedDataImporter: Seeding wind turbines...")
                snapshot.windTurbines.forEach { turbine ->
                    database.windTurbineQueries.upsertWindTurbine(
                        id = turbine.id,
                        wind_park_id = turbine.windParkId,
                        name = turbine.name,
                        municipality_id = turbine.municipalityId,
                        municipality_name = turbine.municipalityName,
                        district_id = turbine.districtId,
                        district_name = turbine.districtName,
                        state_id = turbine.stateId,
                        state_name = turbine.stateName,
                        latitude = turbine.latitude,
                        longitude = turbine.longitude,
                        installed_capacity_kw = turbine.installedCapacityKw,
                        status = turbine.status,
                        turbine_type = turbine.turbineType,
                        manufacturer = turbine.manufacturer,
                        model = turbine.model,
                        hub_height_m = turbine.hubHeightM,
                        rotor_diameter_m = turbine.rotorDiameterM,
                        commissioning_year = turbine.commissioningYear?.toLong(),
                        source_name = turbine.sourceName,
                        source_url = turbine.sourceUrl,
                        source_updated_at = turbine.sourceUpdatedAt,
                        data_quality = turbine.dataQuality,
                    )
                }

                println("SnapshotSeedDataImporter: Seeding metrics from snapshot...")
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

                println("SnapshotSeedDataImporter: Seeding metadata...")
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
                database.settingQueries.upsertSetting(
                    key = SNAPSHOT_METRIC_IMPORT_VERSION_KEY,
                    value_ = SNAPSHOT_METRIC_IMPORT_VERSION,
                )
            }
            println("SnapshotSeedDataImporter: Seeding database completed successfully.")
        } catch (e: Throwable) {
            println("SnapshotSeedDataImporter ERROR: Seeding failed with exception!")
            e.printStackTrace()
            throw e
        }
    }
}
