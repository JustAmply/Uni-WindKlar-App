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
                if (existingSnapshot != null) {
                    println("SnapshotSeedDataImporter: Fast-path checksum matches database. Skipping full JSON parsing and import.")
                    return@withContext
                }
            }

            println("SnapshotSeedDataImporter: Seeding database required. Reading full JSON snapshot...")
            val jsonString = snapshotProvider.readSnapshotJson()
            println("SnapshotSeedDataImporter: Read JSON snapshot. Size = ${jsonString.length} characters.")

            println("SnapshotSeedDataImporter: Decoding full JSON snapshot...")
            val snapshot = json.decodeFromString<WindklarSnapshot>(jsonString)
            println("SnapshotSeedDataImporter: Decoded JSON. Parks = ${snapshot.windParks.size}, Turbines = ${snapshot.windTurbines.size}, Metrics = ${snapshot.metrics.size}")
            
            val metadata = snapshot.snapshotMetadata
            println("SnapshotSeedDataImporter: Checking if snapshot already exists in DB (checksum: ${metadata.checksumSha256})...")
            val existingSnapshot = database.snapshotMetadataQueries
                .selectSnapshotByChecksum(metadata.checksumSha256)
                .executeAsOneOrNull()

            if (existingSnapshot != null) {
                println("SnapshotSeedDataImporter: Snapshot checksum already exists in DB. Skipping import.")
                return@withContext
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

                println("SnapshotSeedDataImporter: Seeding metrics via SQL generators...")
                database.metricQueries.generateProductionMetrics()
                database.metricQueries.generateCo2Metrics()
                database.metricQueries.generateHouseholdMetrics()
                database.metricQueries.generateMunicipalMetrics()

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
            }
            println("SnapshotSeedDataImporter: Seeding database completed successfully.")
        } catch (e: Throwable) {
            println("SnapshotSeedDataImporter ERROR: Seeding failed with exception!")
            e.printStackTrace()
            throw e
        }
    }
}
