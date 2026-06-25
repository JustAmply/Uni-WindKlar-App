package app.feature.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.MapMarkerKind
import app.core.model.MapMarkerUiModel
import app.core.model.MapSearchEntry
import app.core.model.WindTurbine
import app.core.model.WindPark

import app.core.ui.components.EntityType
import app.core.ui.components.EntityPreviewData
import app.core.ui.components.PreviewSheetState
import app.data.repository.WindParkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor

import app.core.location.LocationProvider

private const val SearchDebounceMillis = 80L
private const val SearchResultLimit = 50


class MapViewModel(
    private val repository: WindParkRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {
    var uiState by mutableStateOf(MapUiState(isLoading = true))
        private set

    private var parkStatuses: Map<String, String> = emptyMap()
    private var viewportBounds: MapBounds? = null
    private var filterJob: Job? = null
    private var searchJob: Job? = null

    private var statesList: List<MapSearchResult.State> = emptyList()
    private var districtsList: List<MapSearchResult.District> = emptyList()
    private var municipalitiesList: List<MapSearchResult.Municipality> = emptyList()
    private var searchIndex: List<MapSearchIndexEntry> = emptyList()

    init {
        loadMapData()
    }

    fun loadMapData() {
        viewModelScope.launch {
            try {
                println("MapViewModel: Starting loadMapData...")
                uiState = uiState.copy(isLoading = true)
                
                val allParks = repository.getWindParks()
                println("MapViewModel: Loaded ${allParks.size} wind parks from repository.")
                
                val statusMap = repository.getWindParkStatuses()
                println("MapViewModel: Loaded ${statusMap.size} park statuses from repository.")
                
                parkStatuses = statusMap

                uiState = uiState.copy(
                    isLoading = false,
                    parks = allParks,
                )
                applyFilters()
                loadRecentParks()

                val searchEntries = repository.getMapSearchEntries()
                val parkById = allParks.associateBy { it.id }
                searchIndex = withContext(Dispatchers.Default) {
                    searchEntries.mapNotNull { entry -> entry.toSearchIndexEntry(parkById) }
                }
                statesList = searchIndex.mapNotNull { it.result as? MapSearchResult.State }
                districtsList = searchIndex.mapNotNull { it.result as? MapSearchResult.District }
                municipalitiesList = searchIndex.mapNotNull { it.result as? MapSearchResult.Municipality }
                println("MapViewModel: loadMapData finished successfully.")
            } catch (e: Throwable) {
                println("MapViewModel ERROR: loadMapData failed!")
                e.printStackTrace()
                uiState = uiState.copy(
                    isLoading = false,
                    parks = emptyList(),
                    filteredParks = emptyList()
                )
            }
        }
    }

    fun loadRecentParks() {
        viewModelScope.launch {
            try {
                val recents = repository.getRecentWindParks(5)
                uiState = uiState.copy(recentParks = recents)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun onSearchFocusChanged(focused: Boolean) {
        uiState = uiState.copy(isSearchFocused = focused)
        if (focused) {
            loadRecentParks()
            if (uiState.searchQuery.length >= 2) {
                uiState = uiState.copy(showSearchOverlay = uiState.searchResults.isNotEmpty())
            } else {
                uiState = uiState.copy(showSearchOverlay = uiState.recentParks.isNotEmpty())
            }
        } else {
            uiState = uiState.copy(showSearchOverlay = false)
        }
    }



    fun setStatusFilter(status: String) {
        uiState = uiState.copy(selectedStatus = status)
        applyFilters()
    }

    fun onQueryChange(newQuery: String) {
        uiState = uiState.copy(searchQuery = newQuery)
        searchJob?.cancel()
        val normalizedQuery = newQuery.trim().normalizeForSearch()
        if (normalizedQuery.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(SearchDebounceMillis)
                val combinedResults = withContext(Dispatchers.Default) {
                    searchMapIndex(normalizedQuery)
                }

                if (uiState.searchQuery == newQuery) {
                    uiState = uiState.copy(
                        searchResults = combinedResults,
                        showSearchOverlay = true
                    )
                }
            }
        } else {
            uiState = uiState.copy(
                searchResults = emptyList(),
                showSearchOverlay = uiState.isSearchFocused && uiState.recentParks.isNotEmpty()
            )
        }
    }

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

    private fun searchMapIndex(
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

    fun onSearchResultSelected(result: MapSearchResult) {
        searchJob?.cancel()
        viewportBounds = null
        when (result) {
            is MapSearchResult.State -> {
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 8.0f,
                    selectedPark = null,
                    selectedPreviewData = null,
                    previewSheetState = PreviewSheetState.Expanded,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                loadRegionMetrics(result.id, "state", result.name, null)
            }
            is MapSearchResult.District -> {
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 10.0f,
                    selectedPark = null,
                    selectedPreviewData = null,
                    previewSheetState = PreviewSheetState.Expanded,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                loadRegionMetrics(result.id, "district", result.name, result.stateName)
            }
            is MapSearchResult.Municipality -> {
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 12.0f,
                    selectedPark = null,
                    selectedPreviewData = null,
                    previewSheetState = PreviewSheetState.Expanded,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                loadRegionMetrics(result.id, "city", result.name, "${result.districtName}, ${result.stateName}")
            }
            is MapSearchResult.Park -> {
                val park = result.park
                uiState = uiState.copy(
                    mapCenterLat = park.latitude,
                    mapCenterLon = park.longitude,
                    zoomLevel = 12.0f,
                    selectedPark = park,
                    previewSheetState = PreviewSheetState.Expanded,
                    selectedStatus = "Alle",
                    filteredParks = parksForStatus("Alle"),
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                applyFilters()
                loadParkPreviewData(park)
            }
        }
    }

    fun onParkClicked(park: WindPark) {
        uiState = uiState.copy(
            selectedPark = park,
            previewSheetState = PreviewSheetState.Expanded
        )
        loadParkPreviewData(park)
    }

    fun onParkClickedById(parkId: String) {
        val park = uiState.parks.firstOrNull { it.id == parkId } ?: return
        onParkClicked(park)
    }

    private fun loadParkPreviewData(park: WindPark) {
        viewModelScope.launch {
            try {
                repository.recordRecentWindPark(park.id)
                loadRecentParks()
                val metrics = repository.getMetricsForPark(park.id)
                val annualMetric = metrics.firstOrNull { it.metricType == "annual_production" }
                val co2Metric = metrics.firstOrNull { it.metricType == "co2_savings" }
                val annualGwh = annualMetric?.value?.let { it / 1_000_000.0 }
                val co2Tons = co2Metric?.value?.let { it / 1000.0 }

                uiState = uiState.copy(
                    selectedPreviewData = EntityPreviewData(
                        id = park.id,
                        type = EntityType.PARK,
                        title = park.name,
                        subtitle = "Gemeinde ${park.municipalityName}",
                        badgeLabel = "Aktiv",
                        annualProductionGwh = annualGwh,
                        co2SavingsTons = co2Tons
                    )
                )
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRegionMetrics(id: String, type: String, name: String, parentContext: String?) {
        viewModelScope.launch {
            try {
                val regionSummary = repository
                    .getRegionSummaries(type)
                    .firstOrNull { it.regionId == id }
                val annualProductionGwh = regionSummary?.annualProductionKwh?.let { it / 1_000_000.0 }
                val co2SavingsTons = regionSummary?.co2SavingsKg?.let { it / 1000.0 }
                
                val entityType = when (type.lowercase()) {
                    "city" -> EntityType.CITY
                    "district" -> EntityType.DISTRICT
                    "state" -> EntityType.STATE
                    else -> EntityType.CITY
                }
                
                val badgeLabel = when (entityType) {
                    EntityType.CITY -> "Gemeinde"
                    EntityType.DISTRICT -> "Landkreis"
                    EntityType.STATE -> "Bundesland"
                    else -> "Region"
                }

                val subtitle = when (entityType) {
                    EntityType.CITY -> "Gemeinde in $parentContext"
                    EntityType.DISTRICT -> "Landkreis in $parentContext"
                    EntityType.STATE -> "Bundesland"
                    else -> "Region"
                }

                uiState = uiState.copy(
                    selectedPreviewData = EntityPreviewData(
                        id = id,
                        type = entityType,
                        title = name,
                        subtitle = subtitle,
                        badgeLabel = badgeLabel,
                        annualProductionGwh = annualProductionGwh,
                        co2SavingsTons = co2SavingsTons
                    )
                )
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun submitDataHint(
        category: String,
        confidence: String,
        description: String,
        suggestedValue: String?,
        latitude: Double,
        longitude: Double,
        windParkId: String?,
        municipalityId: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.submitDataHint(
                category = category,
                confidence = confidence,
                description = description,
                status = "ready_for_review",
                windTurbineId = null,
                windParkId = windParkId,
                municipalityId = municipalityId,
                latitude = latitude,
                longitude = longitude,
                suggestedValue = suggestedValue,
                imageUri = null
            )
            onSuccess()
        }
    }

    fun startPinPlacement() {
        val reportPark = uiState.selectedPark
        uiState = uiState.copy(
            isPinPlacementMode = true,
            placementMarkerLat = reportPark?.latitude ?: uiState.mapCenterLat,
            placementMarkerLon = reportPark?.longitude ?: uiState.mapCenterLon,
            pendingReportPark = reportPark,
        )
        applyFilters()
    }

    fun updatePlacementPinLocation(lat: Double, lon: Double) {
        uiState = uiState.copy(
            placementMarkerLat = lat,
            placementMarkerLon = lon
        )
        applyFilters()
    }

    fun cancelPinPlacement() {
        uiState = uiState.copy(
            isPinPlacementMode = false,
            pendingReportPark = null,
        )
        applyFilters()
    }

    fun confirmPinPlacement(onConfirm: (Double, Double) -> Unit) {
        val lat = uiState.placementMarkerLat
        val lon = uiState.placementMarkerLon
        uiState = uiState.copy(isPinPlacementMode = false)
        applyFilters()
        onConfirm(lat, lon)
    }



    fun expandPreview() {
        if (uiState.selectedPark != null || uiState.selectedPreviewData != null) {
            uiState = uiState.copy(previewSheetState = PreviewSheetState.Expanded)
        }
    }

    fun minimizePreview() {
        if (uiState.selectedPark != null || uiState.selectedPreviewData != null) {
            uiState = uiState.copy(previewSheetState = PreviewSheetState.Minimized)
        }
    }

    fun dismissPreview() {
        uiState = uiState.copy(
            selectedPark = null,
            selectedPreviewData = null,
            previewSheetState = PreviewSheetState.Expanded
        )
    }

    fun centerOnLocation(lat: Double, lon: Double) {
        viewportBounds = null
        uiState = uiState.copy(
            mapCenterLat = lat,
            mapCenterLon = lon,
            zoomLevel = 10.0f
        )
        applyFilters()
    }

    fun centerOnUserLocation(onPermissionRequired: () -> Unit, onError: (String) -> Unit) {
        if (!locationProvider.hasPermission()) {
            onPermissionRequired()
            return
        }
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true)
                val location = locationProvider.getCurrentLocation()
                uiState = uiState.copy(isLoading = false)
                if (location != null) {
                    centerOnLocation(location.first, location.second)
                } else {
                    onError("Standort konnte nicht ermittelt werden.")
                }
            } catch (e: Throwable) {
                uiState = uiState.copy(isLoading = false)
                onError("Fehler bei der Ortung: ${e.message ?: e.toString()}")
            }
        }
    }

    fun onZoomChanged(zoom: Float) {
        viewportBounds = null
        uiState = uiState.copy(zoomLevel = zoom.coerceIn(5.0f, 18.0f))
        applyFilters()
    }

    fun onClusterClicked(lat: Double, lon: Double) {
        viewportBounds = null
        uiState = uiState.copy(
            mapCenterLat = lat,
            mapCenterLon = lon,
            zoomLevel = (uiState.zoomLevel + 2.0f).coerceIn(5.0f, 18.0f),
            selectedPark = null,
            selectedPreviewData = null,
            previewSheetState = PreviewSheetState.Expanded,
        )
        applyFilters()
    }

    fun onMapMoved(lat: Double, lon: Double, zoom: Float) {
        viewportBounds = null
        if (abs(uiState.mapCenterLat - lat) > 0.0001 || 
            abs(uiState.mapCenterLon - lon) > 0.0001 || 
            abs(uiState.zoomLevel - zoom) > 0.1) {
            uiState = uiState.copy(
                mapCenterLat = lat,
                mapCenterLon = lon,
                zoomLevel = zoom.coerceIn(5.0f, 18.0f)
            )
            applyFilters()
        }
    }

    fun onMapMovedWithBounds(
        lat: Double,
        lon: Double,
        zoom: Float,
        swLat: Double,
        swLon: Double,
        neLat: Double,
        neLon: Double,
    ) {
        val nextBounds = MapBounds(
            swLat = minOf(swLat, neLat),
            swLon = swLon,
            neLat = maxOf(swLat, neLat),
            neLon = neLon,
        )
        val boundsChanged = viewportBounds?.isCloseTo(nextBounds) != true
        viewportBounds = nextBounds

        if (boundsChanged ||
            abs(uiState.mapCenterLat - lat) > 0.0001 ||
            abs(uiState.mapCenterLon - lon) > 0.0001 ||
            abs(uiState.zoomLevel - zoom) > 0.1
        ) {
            uiState = uiState.copy(
                mapCenterLat = lat,
                mapCenterLon = lon,
                zoomLevel = zoom.coerceIn(5.0f, 18.0f)
            )
            applyFilters()
        }
    }

    fun onPanChanged(deltaLat: Double, deltaLon: Double) {
        uiState = uiState.copy(
            mapCenterLat = uiState.mapCenterLat + deltaLat,
            mapCenterLon = uiState.mapCenterLon + deltaLon
        )
    }

    private fun filterTurbines(turbines: List<WindTurbine>, statusFilter: String): List<WindTurbine> {
        if (statusFilter == "Alle") {
            return turbines.filter { determineTurbineStatus(it.status) != "Stillgelegt" }
        }
        if (statusFilter == "Geplant") {
            return turbines.filter {
                val s = determineTurbineStatus(it.status)
                s == "Geplant" || s == "Im Bau"
            }
        }
        return turbines.filter { determineTurbineStatus(it.status) == statusFilter }
    }

    private fun determineTurbineStatus(status: String?): String {
        if (status == null) return "Aktiv"
        val lower = status.lowercase()
        if (lower.contains("bau") || lower.contains("errichtung")) return "Im Bau"
        if (lower.contains("betrieb") || lower.contains("aktiv")) return "Aktiv"
        if (lower.contains("stillgelegt")) return "Stillgelegt"
        return "Geplant"
    }

    private fun turbinesToMarkers(turbines: List<WindTurbine>): List<MapMarkerUiModel> {
        return turbines.map { turbine ->
            MapMarkerUiModel(
                id = turbine.id,
                latitude = turbine.latitude,
                longitude = turbine.longitude,
                kind = MapMarkerKind.Turbine,
                count = 1,
                parkId = turbine.windParkId
            )
        }
    }

    private fun applyFilters() {
        filterJob?.cancel()
        if (uiState.isPinPlacementMode) {
            uiState = uiState.copy(
                mapMarkers = listOf(
                    MapMarkerUiModel(
                        id = "placement_pin",
                        latitude = uiState.placementMarkerLat,
                        longitude = uiState.placementMarkerLon,
                        kind = MapMarkerKind.PlacementPin,
                        count = 1
                    )
                )
            )
            return
        }

        val snapshot = uiState
        val currentStatus = snapshot.selectedStatus
        val currentStatuses = parkStatuses
        val bounds = viewportBounds
        val turbineBounds = bounds ?: fallbackBounds(snapshot.mapCenterLat, snapshot.mapCenterLon, snapshot.zoomLevel)

        filterJob = viewModelScope.launch {
            try {
                delay(150)
                val filteredParks = withContext(Dispatchers.Default) {
                    val rawParks = parksForStatus(snapshot.parks, currentStatuses, currentStatus)
                    filterParksInBounds(
                        rawParks,
                        bounds,
                    )
                }

                val markers = if (snapshot.zoomLevel > 14.0f) {
                    val turbines = repository.getWindTurbinesInBounds(
                        swLat = turbineBounds.swLat,
                        swLon = turbineBounds.swLon,
                        neLat = turbineBounds.neLat,
                        neLon = turbineBounds.neLon,
                    )
                    withContext(Dispatchers.Default) {
                        val filteredTurbines = filterTurbines(turbines, currentStatus)
                        turbinesToMarkers(filteredTurbines)
                    }
                } else {
                    withContext(Dispatchers.Default) {
                        markersForZoom(filteredParks, snapshot.zoomLevel)
                    }
                }

                uiState = uiState.copy(
                    filteredParks = filteredParks,
                    mapMarkers = markers
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                uiState = uiState.copy(
                    filteredParks = emptyList(),
                    mapMarkers = emptyList()
                )
            }
        }
    }

    private fun parksForStatus(status: String): List<WindPark> =
        parksForStatus(uiState.parks, parkStatuses, status)

    private fun parksForStatus(
        parks: List<WindPark>,
        statuses: Map<String, String>,
        status: String,
    ): List<WindPark> =
        if (status == "Alle") {
            parks.filter { statusForPark(statuses, it.id) != "Stillgelegt" }
        } else if (status == "Geplant") {
            parks.filter {
                val s = statusForPark(statuses, it.id)
                s == "Geplant" || s == "Im Bau"
            }
        } else {
            parks.filter { park -> statusForPark(statuses, park.id) == status }
        }

    private fun statusForPark(parkId: String): String =
        statusForPark(parkStatuses, parkId)

    private fun statusForPark(statuses: Map<String, String>, parkId: String): String =
        statuses[parkId] ?: "Aktiv"

    private fun filterParksInBounds(parks: List<WindPark>, bounds: MapBounds?): List<WindPark> =
        bounds?.let { mapBounds ->
            parks.filter { park -> mapBounds.contains(park.latitude, park.longitude) }
        } ?: parks

    private fun markersForZoom(parks: List<WindPark>, zoom: Float): List<MapMarkerUiModel> {
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
                    val lat = bucketParks.map { it.latitude }.average()
                    val lon = bucketParks.map { it.longitude }.average()
                    MapMarkerUiModel(
                        id = "cluster_${gridSize}_${bucket.first}_${bucket.second}",
                        latitude = lat,
                        longitude = lon,
                        kind = MapMarkerKind.Cluster,
                        count = bucketParks.size,
                    )
                }
            }
    }

    private fun fallbackBounds(centerLat: Double, centerLon: Double, zoom: Float): MapBounds {
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

    private data class MapBounds(
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
}

private data class MapSearchIndexEntry(
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

private data class SearchMatch(
    val entry: MapSearchIndexEntry,
    val matchRank: Int,
)

private fun String.normalizeForSearch(): String =
    trim().lowercase()
