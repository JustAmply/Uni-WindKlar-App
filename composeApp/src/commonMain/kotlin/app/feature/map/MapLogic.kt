package app.feature.map

import app.core.model.MapMarkerKind
import app.core.model.MapMarkerUiModel
import app.core.model.MapSearchEntry
import app.core.model.WindPark
import app.core.model.WindTurbine
import app.data.repository.MapStartupSnapshot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

private const val SearchResultLimit = 50
internal const val TurbineMarkerMinZoom = 12.0f
private const val TileSizePx = 256.0
private const val FallbackViewportHeightPx = 1120.0
private const val FallbackViewportWidthPx = 900.0
private const val SmallClusterCollisionSpanPx = 16.0

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
    if (parks.isEmpty()) {
        return emptyList()
    }

    if (zoom >= TurbineMarkerMinZoom) {
        return parks.map { it.toParkMarker() }
    }

    val clusterRadius = clusterRadiusPixels(zoom)
    val projectedBuckets = linkedMapOf<Pair<Int, Int>, MutableList<ProjectedPark>>()
    parks.forEach { park ->
        val projectedPark = park.projectForZoom(zoom)
        val key = floor(projectedPark.x / clusterRadius).toInt() to
            floor(projectedPark.y / clusterRadius).toInt()
        projectedBuckets.getOrPut(key) { mutableListOf() }.add(projectedPark)
    }

    val zoomBand = (zoom * 10).toInt()
    return projectedBuckets.flatMap { (bucket, bucketParks) ->
        if (bucketParks.shouldClusterAt(zoom)) {
            listOf(
                MapMarkerUiModel(
                    id = "cluster_${zoomBand}_${bucket.first}_${bucket.second}",
                    latitude = bucketParks.map { it.park.latitude }.average(),
                    longitude = bucketParks.map { it.park.longitude }.average(),
                    kind = MapMarkerKind.Cluster,
                    count = bucketParks.size,
                )
            )
        } else {
            bucketParks.map { it.park.toParkMarker() }
        }
    }
}

internal fun fallbackBounds(centerLat: Double, centerLon: Double, zoom: Float): MapBounds {
    val latSpan = fallbackVisibleLatitudeSpan(centerLat, zoom)
    val lonSpan = fallbackVisibleLongitudeSpan(zoom)
    return MapBounds(
        swLat = centerLat - latSpan / 2.0,
        swLon = centerLon - lonSpan / 2.0,
        neLat = centerLat + latSpan / 2.0,
        neLon = centerLon + lonSpan / 2.0,
    )
}

internal fun fallbackVisibleLatitudeSpan(centerLat: Double, zoom: Float): Double {
    val latitudeCorrection = cos(centerLat.coerceIn(-85.0, 85.0) * PI / 180.0)
        .coerceAtLeast(0.25)
    return (360.0 / mapScale(zoom) * FallbackViewportHeightPx * latitudeCorrection)
        .coerceIn(0.003, 30.0)
}

private fun fallbackVisibleLongitudeSpan(zoom: Float): Double =
    (360.0 / mapScale(zoom) * FallbackViewportWidthPx)
        .coerceIn(0.003, 30.0)

private data class ProjectedPark(
    val park: WindPark,
    val x: Double,
    val y: Double,
)

private fun WindPark.toParkMarker() =
    MapMarkerUiModel(
        id = id,
        latitude = latitude,
        longitude = longitude,
        kind = MapMarkerKind.Park,
        count = 1,
        parkId = id,
    )

private fun WindPark.projectForZoom(zoom: Float): ProjectedPark {
    val scale = mapScale(zoom)
    return ProjectedPark(
        park = this,
        x = xOffset * scale,
        y = yOffset * scale,
    )
}

private fun List<ProjectedPark>.shouldClusterAt(zoom: Float): Boolean {
    if (size < 2) return false
    if (size >= denseClusterMinimum(zoom)) return true
    return projectedSpanPx() <= SmallClusterCollisionSpanPx
}

private fun List<ProjectedPark>.projectedSpanPx(): Double {
    val minX = minOf { it.x }
    val maxX = maxOf { it.x }
    val minY = minOf { it.y }
    val maxY = maxOf { it.y }
    return maxOf(maxX - minX, maxY - minY)
}

private fun denseClusterMinimum(zoom: Float): Int =
    when {
        zoom < 7.0f -> 4
        zoom < 9.0f -> 5
        else -> 6
    }

private fun clusterRadiusPixels(zoom: Float): Double =
    when {
        zoom < 7.0f -> 56.0
        zoom < 9.0f -> 46.0
        zoom < 11.0f -> 38.0
        else -> 32.0
    }

private fun mapScale(zoom: Float): Double =
    TileSizePx * 2.0.pow(zoom.toDouble())

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
