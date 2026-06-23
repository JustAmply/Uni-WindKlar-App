package app.feature.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.MapMarkerKind
import app.core.model.MapMarkerUiModel
import app.core.model.WindTurbine
import app.core.model.WindPark
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

                // Pre-aggregate locations for search hierarchy
                val stateCenters = mutableMapOf<String, LocationCenter>()
                val districtCenters = mutableMapOf<String, Pair<String, LocationCenter>>()
                val municipalityCenters = mutableMapOf<String, Triple<String, String, LocationCenter>>()

                allParks.forEach { park ->
                    val sName = park.stateName.trim()
                    if (sName.isNotEmpty()) {
                        val c = stateCenters.getOrPut(sName) { LocationCenter(0.0, 0.0, 0) }
                        c.sumLat += park.latitude
                        c.sumLon += park.longitude
                        c.count += 1
                    }

                    val dName = park.districtName.trim()
                    if (dName.isNotEmpty() && sName.isNotEmpty()) {
                        val key = "$sName|$dName"
                        val pair = districtCenters.getOrPut(key) { sName to LocationCenter(0.0, 0.0, 0) }
                        pair.second.sumLat += park.latitude
                        pair.second.sumLon += park.longitude
                        pair.second.count += 1
                    }

                    val mName = park.municipalityName.trim()
                    if (mName.isNotEmpty() && dName.isNotEmpty() && sName.isNotEmpty()) {
                        val key = "$sName|$dName|$mName"
                        val triple = municipalityCenters.getOrPut(key) { Triple(sName, dName, LocationCenter(0.0, 0.0, 0)) }
                        triple.third.sumLat += park.latitude
                        triple.third.sumLon += park.longitude
                        triple.third.count += 1
                    }
                }

                statesList = stateCenters.map { (name, center) ->
                    MapSearchResult.State(name, center.sumLat / center.count, center.sumLon / center.count)
                }.sortedBy { it.name }

                districtsList = districtCenters.map { (key, pair) ->
                    val name = key.substringAfter("|")
                    MapSearchResult.District(name, pair.first, pair.second.sumLat / pair.second.count, pair.second.sumLon / pair.second.count)
                }.sortedBy { it.name }

                municipalitiesList = municipalityCenters.map { (key, triple) ->
                    val name = key.substringAfterLast("|")
                    MapSearchResult.Municipality(name, triple.second, triple.first, triple.third.sumLat / triple.third.count, triple.third.sumLon / triple.third.count)
                }.sortedBy { it.name }

                uiState = uiState.copy(
                    isLoading = false,
                    parks = allParks,
                )
                applyFilters()
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



    fun setStatusFilter(status: String) {
        uiState = uiState.copy(selectedStatus = status)
        applyFilters()
    }

    fun onQueryChange(newQuery: String) {
        uiState = uiState.copy(searchQuery = newQuery)
        searchJob?.cancel()
        if (newQuery.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(200)
                val dbParks = repository.searchWindParks(newQuery)
                
                val matchingStates = statesList.filter { it.name.contains(newQuery, ignoreCase = true) }
                val matchingDistricts = districtsList.filter { it.name.contains(newQuery, ignoreCase = true) }
                val matchingMunicipalities = municipalitiesList.filter { it.name.contains(newQuery, ignoreCase = true) }
                val matchingParks = dbParks.map { MapSearchResult.Park(it) }

                val combinedResults = matchingStates + matchingDistricts + matchingMunicipalities + matchingParks

                uiState = uiState.copy(
                    searchResults = combinedResults,
                    showSearchOverlay = true
                )
            }
        } else {
            uiState = uiState.copy(
                searchResults = emptyList(),
                showSearchOverlay = false
            )
        }
    }

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
                    previewSheetState = ParkPreviewSheetState.Minimized,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
            }
            is MapSearchResult.District -> {
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 10.0f,
                    selectedPark = null,
                    previewSheetState = ParkPreviewSheetState.Minimized,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
            }
            is MapSearchResult.Municipality -> {
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 12.0f,
                    selectedPark = null,
                    previewSheetState = ParkPreviewSheetState.Minimized,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
            }
            is MapSearchResult.Park -> {
                val park = result.park
                uiState = uiState.copy(
                    mapCenterLat = park.latitude,
                    mapCenterLon = park.longitude,
                    zoomLevel = 12.0f,
                    selectedPark = park,
                    previewSheetState = ParkPreviewSheetState.Expanded,
                    selectedStatus = "Alle",
                    filteredParks = parksForStatus("Alle"),
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                applyFilters()
                viewModelScope.launch {
                    repository.recordRecentWindPark(park.id)
                    val metrics = repository.getMetricsForPark(park.id)
                    uiState = uiState.copy(selectedParkMetrics = metrics)
                }
            }
        }
    }

    fun onParkClicked(park: WindPark) {
        uiState = uiState.copy(
            selectedPark = park,
            previewSheetState = ParkPreviewSheetState.Expanded
        )
        viewModelScope.launch {
            repository.recordRecentWindPark(park.id)
            val metrics = repository.getMetricsForPark(park.id)
            uiState = uiState.copy(selectedParkMetrics = metrics)
        }
    }

    fun onParkClickedById(parkId: String) {
        val park = uiState.parks.firstOrNull { it.id == parkId } ?: return
        onParkClicked(park)
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
        uiState = uiState.copy(
            isPinPlacementMode = true,
            placementMarkerLat = uiState.mapCenterLat,
            placementMarkerLon = uiState.mapCenterLon,
            selectedPark = null,
            selectedParkMetrics = emptyList()
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
        uiState = uiState.copy(isPinPlacementMode = false)
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
        if (uiState.selectedPark != null) {
            uiState = uiState.copy(previewSheetState = ParkPreviewSheetState.Expanded)
        }
    }

    fun minimizePreview() {
        if (uiState.selectedPark != null) {
            uiState = uiState.copy(previewSheetState = ParkPreviewSheetState.Minimized)
        }
    }

    fun dismissPreview() {
        uiState = uiState.copy(
            selectedPark = null,
            previewSheetState = ParkPreviewSheetState.Expanded,
            selectedParkMetrics = emptyList()
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
            selectedParkMetrics = emptyList(),
            previewSheetState = ParkPreviewSheetState.Expanded,
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
                val filteredParks = withContext(Dispatchers.Default) {
                    filterParksInBounds(
                        parksForStatus(snapshot.parks, currentStatuses, currentStatus),
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
                        turbinesToMarkers(filterTurbines(turbines, currentStatus))
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

private data class LocationCenter(
    var sumLat: Double,
    var sumLon: Double,
    var count: Int
)
