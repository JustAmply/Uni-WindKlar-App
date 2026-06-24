package app.data.seed

import app.data.local.db.AppDatabase
import app.data.snapshot.MetricDto
import app.data.snapshot.SnapshotProvider
import app.data.snapshot.WindklarSnapshot
import app.data.snapshot.WindParkDto
import app.data.snapshot.WindTurbineDto
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
        const val SNAPSHOT_IMPORT_VERSION_KEY = "snapshot_source_import_version"
        const val SNAPSHOT_IMPORT_VERSION = "snapshot_source_v2_commissioning_year"
    }

    override suspend fun importIfNeeded(onProgress: (ImportProgress) -> Unit): Unit = withContext(Dispatchers.Default) {
        println("SnapshotSeedDataImporter: Starting importIfNeeded...")
        onProgress(ImportProgress.CheckingChecksum)
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
                val hasCurrentSnapshotImport = database.settingQueries
                    .getSetting(SNAPSHOT_IMPORT_VERSION_KEY)
                    .executeAsOneOrNull() == SNAPSHOT_IMPORT_VERSION
                if (existingSnapshot != null && hasCurrentSnapshotImport) {
                    println("SnapshotSeedDataImporter: Fast-path checksum matches database. Skipping full JSON parsing and import.")
                    onProgress(ImportProgress.Completed)
                    return@withContext
                }
                if (existingSnapshot != null) {
                    println("SnapshotSeedDataImporter: Checksum exists, but source import version is outdated. Repairing snapshot data.")
                }
            }

            println("SnapshotSeedDataImporter: Seeding database required. Reading full JSON snapshot...")
            onProgress(ImportProgress.ReadingJson)
            val jsonString = snapshotProvider.readSnapshotJson()
            println("SnapshotSeedDataImporter: Read JSON snapshot. Size = ${jsonString.length} characters.")

            println("SnapshotSeedDataImporter: Decoding full JSON snapshot...")
            onProgress(ImportProgress.DecodingJson)
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
            val hasCurrentSnapshotImport = database.settingQueries
                .getSetting(SNAPSHOT_IMPORT_VERSION_KEY)
                .executeAsOneOrNull() == SNAPSHOT_IMPORT_VERSION

            if (existingSnapshot != null && hasCurrentSnapshotImport) {
                println("SnapshotSeedDataImporter: Snapshot checksum already exists in DB. Skipping import.")
                onProgress(ImportProgress.Completed)
                return@withContext
            }
            val importMode = if (existingSnapshot != null) {
                println("SnapshotSeedDataImporter: Snapshot checksum exists, but source import version is outdated. Repairing rows in place.")
                ImportMode.RepairSameSnapshot
            } else {
                println("SnapshotSeedDataImporter: New snapshot checksum detected. Replacing source-owned snapshot data.")
                ImportMode.ReplaceSnapshot
            }

            println("SnapshotSeedDataImporter: Seeding database within transaction...")
            database.transaction {
                val preservedUserData = if (importMode == ImportMode.ReplaceSnapshot) {
                    preserveUserData()
                } else {
                    null
                }

                if (importMode == ImportMode.ReplaceSnapshot) {
                    clearSourceOwnedSnapshotData()
                }

                seedSnapshot(snapshot, onProgress)

                if (preservedUserData != null) {
                    restoreUserData(preservedUserData, snapshot)
                }
            }
            onProgress(ImportProgress.Completed)
            println("SnapshotSeedDataImporter: Seeding database completed successfully.")
        } catch (e: Throwable) {
            println("SnapshotSeedDataImporter ERROR: Seeding failed with exception!")
            e.printStackTrace()
            throw e
        }
    }

    private fun seedSnapshot(
        snapshot: WindklarSnapshot,
        onProgress: (ImportProgress) -> Unit,
    ) {
        println("SnapshotSeedDataImporter: Seeding wind parks...")
        val totalParks = snapshot.windParks.size
        snapshot.windParks.forEachIndexed { index, park ->
            if (index % 100 == 0 || index == totalParks - 1) {
                onProgress(ImportProgress.SeedingParks(index + 1, totalParks))
            }
            upsertWindPark(park)
        }

        println("SnapshotSeedDataImporter: Seeding wind turbines...")
        val totalTurbines = snapshot.windTurbines.size
        snapshot.windTurbines.forEachIndexed { index, turbine ->
            if (index % 500 == 0 || index == totalTurbines - 1) {
                onProgress(ImportProgress.SeedingTurbines(index + 1, totalTurbines))
            }
            upsertWindTurbine(turbine)
        }

        println("SnapshotSeedDataImporter: Seeding metrics from snapshot...")
        val totalMetrics = snapshot.metrics.size
        snapshot.metrics.forEachIndexed { index, metric ->
            if (index % 1000 == 0 || index == totalMetrics - 1) {
                onProgress(ImportProgress.SeedingMetrics(index + 1, totalMetrics))
            }
            upsertMetric(metric)
        }

        println("SnapshotSeedDataImporter: Seeding metadata...")
        onProgress(ImportProgress.SeedingMetadata)
        val metadata = snapshot.snapshotMetadata
        val assumptionsJson = json.encodeToString(snapshot.assumptions)
        val limitations = metadata.limitations.joinToString(separator = "\n")
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
            assumptions_json = assumptionsJson,
            limitations = limitations,
            imported_at = metadata.processedAt,
        )
        database.snapshotMetadataQueries.updateSnapshotMetadata(
            snapshot_id = metadata.snapshotId,
            schema_version = snapshot.schemaVersion,
            source_name = metadata.sourceName,
            source_url = metadata.sourceUrl,
            attribution = metadata.attribution,
            mastr_export_date = metadata.mastrExportDate,
            processed_at = metadata.processedAt,
            pipeline_version = metadata.pipelineVersion,
            checksum_sha256 = metadata.checksumSha256,
            assumptions_json = assumptionsJson,
            limitations = limitations,
            imported_at = metadata.processedAt,
        )
        database.settingQueries.upsertSetting(
            key = SNAPSHOT_IMPORT_VERSION_KEY,
            value_ = SNAPSHOT_IMPORT_VERSION,
        )
        database.settingQueries.updateSetting(SNAPSHOT_IMPORT_VERSION, SNAPSHOT_IMPORT_VERSION_KEY)
    }

    private fun upsertWindPark(park: WindParkDto) {
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
        database.windParkQueries.updateWindPark(
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

    private fun upsertWindTurbine(turbine: WindTurbineDto) {
        val commissioningYear = turbine.commissioningYear?.toLong()
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
            commissioning_year = commissioningYear,
            source_name = turbine.sourceName,
            source_url = turbine.sourceUrl,
            source_updated_at = turbine.sourceUpdatedAt,
            data_quality = turbine.dataQuality,
        )
        database.windTurbineQueries.updateWindTurbine(
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
            commissioning_year = commissioningYear,
            source_name = turbine.sourceName,
            source_url = turbine.sourceUrl,
            source_updated_at = turbine.sourceUpdatedAt,
            data_quality = turbine.dataQuality,
        )
    }

    private fun upsertMetric(metric: MetricDto) {
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
        database.metricQueries.updateMetric(
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

    private fun clearSourceOwnedSnapshotData() {
        println("SnapshotSeedDataImporter: Clearing source-owned snapshot tables before replacement.")
        database.metricQueries.deleteAllMetrics()
        database.windTurbineQueries.deleteAllWindTurbines()
        database.windParkQueries.deleteAllWindParks()
        database.snapshotMetadataQueries.deleteAllSnapshotMetadata()
    }

    private fun preserveUserData(): PreservedUserData {
        println("SnapshotSeedDataImporter: Preserving local user data before snapshot replacement.")
        return PreservedUserData(
            favoriteWindParks = database.favoriteQueries.selectFavoriteRows().executeAsList().map {
                PreservedFavoriteWindPark(
                    windParkId = it.wind_park_id,
                    createdAtEpochMillis = it.created_at_epoch_millis,
                )
            },
            favoriteRegions = database.favoriteQueries.selectFavoriteRegions().executeAsList().map {
                PreservedFavoriteRegion(
                    regionType = it.region_type,
                    regionId = it.region_id,
                    createdAtEpochMillis = it.created_at_epoch_millis,
                )
            },
            recentWindParks = database.recentWindParkQueries.selectRecentWindParkRows().executeAsList().map {
                PreservedRecentWindPark(
                    windParkId = it.wind_park_id,
                    openedAtEpochMillis = it.opened_at_epoch_millis,
                )
            },
            dataHints = database.dataHintQueries.selectDataHints().executeAsList().map {
                PreservedDataHint(
                    id = it.id,
                    category = it.category,
                    confidence = it.confidence,
                    status = it.status,
                    description = it.description,
                    windTurbineId = it.wind_turbine_id,
                    windParkId = it.wind_park_id,
                    municipalityId = it.municipality_id,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    suggestedValue = it.suggested_value,
                    imageUri = it.image_uri,
                    createdAtEpochMillis = it.created_at_epoch_millis,
                    updatedAtEpochMillis = it.updated_at_epoch_millis,
                )
            },
        )
    }

    private fun restoreUserData(
        preserved: PreservedUserData,
        snapshot: WindklarSnapshot,
    ) {
        println("SnapshotSeedDataImporter: Restoring local user data after snapshot replacement.")
        val validParkIds = snapshot.windParks.mapTo(mutableSetOf()) { it.id }
        val validTurbineIds = snapshot.windTurbines.mapTo(mutableSetOf()) { it.id }
        val validCityIds = snapshot.windParks.mapTo(mutableSetOf()) { it.municipalityId }
        val validDistrictIds = snapshot.windParks.mapTo(mutableSetOf()) { it.districtId }
        val validStateIds = snapshot.windParks.mapTo(mutableSetOf()) { it.stateId }

        preserved.favoriteWindParks
            .filter { it.windParkId in validParkIds }
            .forEach {
                database.favoriteQueries.addFavorite(it.windParkId, it.createdAtEpochMillis)
                database.favoriteQueries.updateFavoriteTimestamp(it.createdAtEpochMillis, it.windParkId)
            }

        preserved.favoriteRegions
            .filter { favorite ->
                when (favorite.regionType.lowercase()) {
                    "city" -> favorite.regionId in validCityIds
                    "district" -> favorite.regionId in validDistrictIds
                    "state" -> favorite.regionId in validStateIds
                    else -> false
                }
            }
            .forEach {
                database.favoriteQueries.addRegionFavorite(
                    it.regionType,
                    it.regionId,
                    it.createdAtEpochMillis,
                )
                database.favoriteQueries.updateRegionFavoriteTimestamp(
                    it.createdAtEpochMillis,
                    it.regionType,
                    it.regionId,
                )
            }

        preserved.recentWindParks
            .filter { it.windParkId in validParkIds }
            .forEach {
                database.recentWindParkQueries.recordRecentWindPark(it.windParkId, it.openedAtEpochMillis)
                database.recentWindParkQueries.updateRecentWindParkTimestamp(it.openedAtEpochMillis, it.windParkId)
            }

        preserved.dataHints.forEach { hint ->
            val windTurbineId = hint.windTurbineId?.takeIf { it in validTurbineIds }
            val windParkId = hint.windParkId?.takeIf { it in validParkIds }
            database.dataHintQueries.upsertDataHint(
                id = hint.id,
                category = hint.category,
                confidence = hint.confidence,
                status = hint.status,
                description = hint.description,
                wind_turbine_id = windTurbineId,
                wind_park_id = windParkId,
                municipality_id = hint.municipalityId,
                latitude = hint.latitude,
                longitude = hint.longitude,
                suggested_value = hint.suggestedValue,
                image_uri = hint.imageUri,
                created_at_epoch_millis = hint.createdAtEpochMillis,
                updated_at_epoch_millis = hint.updatedAtEpochMillis,
            )
            database.dataHintQueries.updateDataHint(
                id = hint.id,
                category = hint.category,
                confidence = hint.confidence,
                status = hint.status,
                description = hint.description,
                wind_turbine_id = windTurbineId,
                wind_park_id = windParkId,
                municipality_id = hint.municipalityId,
                latitude = hint.latitude,
                longitude = hint.longitude,
                suggested_value = hint.suggestedValue,
                image_uri = hint.imageUri,
                created_at_epoch_millis = hint.createdAtEpochMillis,
                updated_at_epoch_millis = hint.updatedAtEpochMillis,
            )
        }
    }

    private enum class ImportMode {
        RepairSameSnapshot,
        ReplaceSnapshot,
    }

    private data class PreservedUserData(
        val favoriteWindParks: List<PreservedFavoriteWindPark>,
        val favoriteRegions: List<PreservedFavoriteRegion>,
        val recentWindParks: List<PreservedRecentWindPark>,
        val dataHints: List<PreservedDataHint>,
    )

    private data class PreservedFavoriteWindPark(
        val windParkId: String,
        val createdAtEpochMillis: Long,
    )

    private data class PreservedFavoriteRegion(
        val regionType: String,
        val regionId: String,
        val createdAtEpochMillis: Long,
    )

    private data class PreservedRecentWindPark(
        val windParkId: String,
        val openedAtEpochMillis: Long,
    )

    private data class PreservedDataHint(
        val id: String,
        val category: String,
        val confidence: String,
        val status: String,
        val description: String,
        val windTurbineId: String?,
        val windParkId: String?,
        val municipalityId: String?,
        val latitude: Double?,
        val longitude: Double?,
        val suggestedValue: String?,
        val imageUri: String?,
        val createdAtEpochMillis: Long,
        val updatedAtEpochMillis: Long,
    )
}
