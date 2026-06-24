package app.data.seed

interface SeedDataImporter {
    suspend fun importIfNeeded(onProgress: (ImportProgress) -> Unit = {})
}

sealed interface ImportProgress {
    data object CheckingChecksum : ImportProgress
    data object ReadingJson : ImportProgress
    data object DecodingJson : ImportProgress
    data class SeedingParks(val current: Int, val total: Int) : ImportProgress
    data class SeedingTurbines(val current: Int, val total: Int) : ImportProgress
    data class SeedingMetrics(val current: Int, val total: Int) : ImportProgress
    data object SeedingMetadata : ImportProgress
    data object Completed : ImportProgress
}
