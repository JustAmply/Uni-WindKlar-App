package app.feature.map

import app.core.model.MapMarkerKind
import app.core.model.MapMarkerUiModel
import app.core.model.MapSearchEntry
import app.core.model.WindPark
import app.core.model.WindTurbine
import app.data.repository.MapStartupSnapshot
import kotlin.math.abs
import kotlin.math.floor

private const val SearchResultLimit = 50

internal data class InitialMapData(
    val searchIndex: List<MapSearchIndexEntry>,
    val filteredParks: List<WindPark>,
    val mapMarkers: List<MapMarkerUiModel>,
)

internal data class MapBounds(
    val swLat: Double,
    val swLon: Double,
    val neLat: Double,
    val neLon: Double,
) {
    fun contains(latitude: Double, longitude: Double): Boolean {
        val inLatitude = latitude in swLat..neLat
        val inLongitude = if (swLon <= neLon) {
            longitude in swLon..neLon
        } else {
            longitude >= swLon || longitude <= neLon
        }
        return inLatitude && inLongitude
    }

    fun isCloseTo(other: MapBounds): Boolean =
        abs(swLat - other.swLat) <= 0.0001 &&
            abs(swLon - other.swLon) <= 0.0001 &&
            abs(neLat - other.neLat) <= 0.0001 &&
            abs(neLon - other.neLon) <= 0.0001
}

internal fun buildInitialMapData(
    snapshot: MapStartupSnapshot,
    filters: MapFilterState,
    zoom: Float,
): InitialMapData {
    val parkById = snapshot.parks.associateBy { it.id }
    val searchIndex = snapshot.searchEntries.mapNotNull { entry ->
        entry.toSearchIndexEntry(parkById)
    }
    val filteredParks = applyMapFilters(
        parks = snapshot.parks,
        statuses = snapshot.parkStatuses,
        filters = filters,
    )
    return InitialMapData(
        searchIndex = searchIndex,
        filteredParks = filteredParks,
        mapMarkers = markersForZoom(filteredParks, zoom),
    )
}

internal fun searchMapIndex(
    searchIndex: List<MapSearchIndexEntry>,
    normalizedQuery: String,
): List<MapSearchResult> =
    searchIndex
        .asSequence()
        .mapNotNull { entry ->
            entry.matchRank(normalizedQuery)?.let { matchRank ->
                SearchMatch(entry, matchRank)
            }
        }
        .sortedWith(
            compareBy<SearchMatch>(
                { it.entry.typeRank },
                { it.matchRank },
                { it.entry.sortName.lowercase() },
                { it.entry.id },
            )
        )
        .take(SearchResultLimit)
        .map { it.entry.result }
        .toList()

internal fun applyMapFilters(
    parks: List<WindPark>,
    statuses: Map<String, String>,
    filters: MapFilterState,
): List<WindPark> =
    parks.filter { park ->
        statusMatches(
            status = statusForPark(statuses, park.id),
            filters = filters,
        ) &&
            filters.sizeRange.matches(park.turbineCount) &&
            filters.capacityRange.matches(park.installedCapacityKw)
    }

internal fun filterParksInBounds(parks: List<WindPark>, bounds: MapBounds?): List<WindPark> =
    bounds?.let { mapBounds ->
        parks.filter { park -> mapBounds.contains(park.latitude, park.longitude) }
    } ?: parks

internal fun filterTurbines(turbines: List<WindTurbine>, filters: MapFilterState): List<WindTurbine> =
    turbines.filter { turbine ->
        statusMatches(
            status = determineTurbineStatus(turbine.status),
            filters = filters,
        )
    }

internal fun turbinesToMarkers(turbines: List<WindTurbine>): List<MapMarkerUiModel> =
    turbines.map { turbine ->
        MapMarkerUiModel(
            id = turbine.id,
            latitude = turbine.latitude,
            longitude = turbine.longitude,
            kind = MapMarkerKind.Turbine,
            count = 1,
            parkId = turbine.windParkId,
        )
    }

internal fun markersForZoom(parks: List<WindPark>, zoom: Float): List<MapMarkerUiModel> {
    val gridSize = when {
        zoom < 6.5f -> 1.5
        zoom < 7.5f -> 1.0
        zoom < 8.5f -> 0.65
        zoom < 9.5f -> 0.4
        zoom < 10.25f -> 0.22
        else -> null
    }

    if (gridSize == null) {
        return parks.map { park ->
            MapMarkerUiModel(
                id = park.id,
                latitude = park.latitude,
                longitude = park.longitude,
                kind = MapMarkerKind.Park,
                count = 1,
                parkId = park.id,
            )
        }
    }

    return parks
        .groupBy { park ->
            val latBucket = floor(park.latitude / gridSize).toInt()
            val lonBucket = floor(park.longitude / gridSize).toInt()
            latBucket to lonBucket
        }
        .map { (bucket, bucketParks) ->
            if (bucketParks.size == 1) {
                val park = bucketParks.first()
                MapMarkerUiModel(
                    id = park.id,
                    latitude = park.latitude,
                    longitude = park.longitude,
                    kind = MapMarkerKind.Park,
                    count = 1,
                    parkId = park.id,
                )
            } else {
                MapMarkerUiModel(
                    id = "cluster_${gridSize}_${bucket.first}_${bucket.second}",
                    latitude = bucketParks.map { it.latitude }.average(),
                    longitude = bucketParks.map { it.longitude }.average(),
                    kind = MapMarkerKind.Cluster,
                    count = bucketParks.size,
                )
            }
        }
}

internal fun fallbackBounds(centerLat: Double, centerLon: Double, zoom: Float): MapBounds {
    val latSpan = when {
        zoom > 16.0f -> 0.04
        zoom > 15.0f -> 0.08
        zoom > 14.0f -> 0.16
        else -> 10.0
    }
    val lonSpan = latSpan * 1.5
    return MapBounds(
        swLat = centerLat - latSpan,
        swLon = centerLon - lonSpan,
        neLat = centerLat + latSpan,
        neLon = centerLon + lonSpan,
    )
}

internal data class MapSearchIndexEntry(
    val result: MapSearchResult,
    val typeRank: Int,
    val id: String,
    val name: String,
    val haystack: String,
    val sortName: String,
) {
    fun matchRank(query: String): Int? =
        when {
            id == query || name == query -> 0
            id.startsWith(query) || name.startsWith(query) -> 1
            haystack.contains(query) -> 2
            else -> null
        }
}

internal fun String.normalizeForSearch(): String =
    trim().lowercase()

private data class SearchMatch(
    val entry: MapSearchIndexEntry,
    val matchRank: Int,
)

private fun MapSearchEntry.toSearchIndexEntry(parkById: Map<String, WindPark>): MapSearchIndexEntry? {
    val result = when (resultType) {
        "state" -> MapSearchResult.State(targetId, label, latitude, longitude)
        "district" -> MapSearchResult.District(targetId, label, description.removePrefix("Landkreis in "), latitude, longitude)
        "city" -> {
            val parts = description.removePrefix("Gemeinde in ").split(", ")
            MapSearchResult.Municipality(
                id = targetId,
                name = label,
                districtName = parts.getOrNull(0).orEmpty(),
                stateName = parts.getOrNull(1).orEmpty(),
                latitude = latitude,
                longitude = longitude,
            )
        }
        "park" -> parkById[targetId]?.let(MapSearchResult::Park)
        else -> null
    } ?: return null

    return MapSearchIndexEntry(
        result = result,
        typeRank = typeRank,
        id = targetId.normalizeForSearch(),
        name = label.normalizeForSearch(),
        haystack = haystack,
        sortName = sortName,
    )
}

private fun statusForPark(statuses: Map<String, String>, parkId: String): String =
    statuses[parkId] ?: "Aktiv"

private fun statusMatches(status: String, filters: MapFilterState): Boolean {
    if (!filters.includeDecommissioned && status == "Stillgelegt") {
        return false
    }

    return when (filters.status) {
        MapStatusFilter.All -> true
        MapStatusFilter.Active -> status == "Aktiv"
        MapStatusFilter.Planned -> status == "Geplant" || status == "Im Bau"
    }
}

private fun determineTurbineStatus(status: String?): String {
    if (status == null) return "Aktiv"
    val lower = status.lowercase()
    if (lower.contains("bau") || lower.contains("errichtung")) return "Im Bau"
    if (lower.contains("betrieb") || lower.contains("aktiv")) return "Aktiv"
    if (lower.contains("stillgelegt")) return "Stillgelegt"
    return "Geplant"
}
