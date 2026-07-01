package app.data.local.dao

import app.core.model.WindPark
import app.core.model.MapSearchEntry
import app.core.model.RegionSummary
import app.core.model.NationalStatsSummary

data class ParkOperationalSummary(
    val parkStatus: String,
    val turbineCount: Int,
    val capacityKw: Long,
)

interface SummaryDao {
    suspend fun getMapStartupParks(favorites: Set<String>): Pair<List<WindPark>, Map<String, String>>
    suspend fun getAllMapSearchEntries(): List<MapSearchEntry>
    suspend fun getAllParkOperationalSummaries(): Map<String, ParkOperationalSummary>
    suspend fun getParkOperationalSummary(parkId: String): ParkOperationalSummary?
    suspend fun getParkOperationalSummariesByIds(parkIds: Collection<String>): Map<String, ParkOperationalSummary>
    suspend fun getRegionSummary(type: String, id: String): RegionSummary?
    suspend fun getRegionSummaries(type: String): List<RegionSummary>
    suspend fun getNationalStatsSummary(): NationalStatsSummary?
}
