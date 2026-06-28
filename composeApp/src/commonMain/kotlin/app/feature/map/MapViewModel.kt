package app.feature.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.MapMarkerKind
import app.core.model.MapMarkerUiModel
import app.core.model.WindPark
import app.core.model.WindTurbine

import app.core.ui.components.EntityType
import app.core.ui.components.EntityPreviewData
import app.core.ui.components.PreviewSheetState
import app.core.ui.components.PreviewTurbinePoint
import app.core.util.formatGermanNumber
import app.data.repository.DataHintRepository
import app.data.repository.MapRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow

import app.core.location.LocationProvider

private const val SearchDebounceMillis = 80L
private const val SelectedParkSearchZoom = 12.5f
private const val SelectedParkFocusOffsetFraction = 0.22

class MapViewModel(
    private val repository: MapRepository,
    private val dataHintRepository: DataHintRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {
    var uiState by mutableStateOf(MapUiState(isLoading = true))
        private set

    private var parkStatuses: Map<String, String> = emptyMap()
    private var viewportBounds: MapBounds? = null
    private var viewportBoundsZoom: Float? = null
    private var filterJob: Job? = null
    private var searchJob: Job? = null

    private var searchIndex: List<MapSearchIndexEntry> = emptyList()

    init {
        loadMapData()
    }

    fun loadMapData() {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true)

                val snapshot = repository.getMapStartupSnapshot()
                val filters = uiState.filters
                val zoom = uiState.zoomLevel
                val startupData = withContext(Dispatchers.Default) {
                    buildInitialMapData(snapshot, filters, zoom)
                }

                parkStatuses = snapshot.parkStatuses
                searchIndex = startupData.searchIndex
                uiState = uiState.copy(
                    isLoading = false,
                    parks = snapshot.parks,
                    filteredParks = startupData.filteredParks,
                    mapMarkers = startupData.mapMarkers,
                )
                loadRecentParks()
            } catch (e: Throwable) {
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
                // Recents are optional; keep the map usable if the user database is unavailable.
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



    fun setStatusFilter(status: MapStatusFilter) {
        uiState = uiState.copy(filters = uiState.filters.copy(status = status))
        applyFilters()
    }

    fun setIncludeDecommissioned(includeDecommissioned: Boolean) {
        uiState = uiState.copy(
            filters = uiState.filters.copy(includeDecommissioned = includeDecommissioned)
        )
        applyFilters()
    }

    fun setParkSizeRange(sizeRange: MapParkSizeRange) {
        uiState = uiState.copy(filters = uiState.filters.copy(sizeRange = sizeRange))
        applyFilters()
    }

    fun setCapacityRange(capacityRange: MapCapacityRange) {
        uiState = uiState.copy(filters = uiState.filters.copy(capacityRange = capacityRange))
        applyFilters()
    }

    fun resetMapFilters() {
        uiState = uiState.copy(filters = MapFilterState())
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
                    searchMapIndex(searchIndex, normalizedQuery)
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

    fun onSearchResultSelected(result: MapSearchResult) {
        searchJob?.cancel()
        when (result) {
            is MapSearchResult.State -> {
                clearViewportBounds()
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 8.0f,
                    selectedPark = null,
                    selectedPreviewData = null,
                    previewSheetState = PreviewSheetState.Hidden,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                loadRegionMetrics(result.id, "state", result.name, null)
            }
            is MapSearchResult.District -> {
                clearViewportBounds()
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 10.0f,
                    selectedPark = null,
                    selectedPreviewData = null,
                    previewSheetState = PreviewSheetState.Hidden,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                loadRegionMetrics(result.id, "district", result.name, result.stateName)
            }
            is MapSearchResult.Municipality -> {
                clearViewportBounds()
                uiState = uiState.copy(
                    mapCenterLat = result.latitude,
                    mapCenterLon = result.longitude,
                    zoomLevel = 12.0f,
                    selectedPark = null,
                    selectedPreviewData = null,
                    previewSheetState = PreviewSheetState.Hidden,
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                loadRegionMetrics(result.id, "city", result.name, "${result.districtName}, ${result.stateName}")
            }
            is MapSearchResult.Park -> {
                val park = result.park
                val zoom = SelectedParkSearchZoom
                val focusedCenter = previewFocusedCenter(
                    latitude = park.latitude,
                    longitude = park.longitude,
                    zoom = zoom,
                )
                clearViewportBounds()
                uiState = uiState.copy(
                    mapCenterLat = focusedCenter.first,
                    mapCenterLon = focusedCenter.second,
                    zoomLevel = zoom,
                    selectedPark = park,
                    previewSheetState = PreviewSheetState.Peek,
                    filters = MapFilterState(),
                    showSearchOverlay = false,
                    searchQuery = ""
                )
                applyFilters()
                loadParkPreviewData(park)
            }
        }
    }

    fun onParkClicked(park: WindPark) {
        val zoom = maxOf(uiState.zoomLevel, TurbineMarkerMinZoom)
        val focusedCenter = previewFocusedCenter(
            latitude = park.latitude,
            longitude = park.longitude,
            zoom = zoom,
        )
        clearViewportBounds()
        uiState = uiState.copy(
            mapCenterLat = focusedCenter.first,
            mapCenterLon = focusedCenter.second,
            zoomLevel = zoom,
            selectedPark = park,
            previewSheetState = PreviewSheetState.Peek
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
                val turbines = repository.getWindTurbinesForPark(park.id)
                val annualMetric = metrics.firstOrNull { it.metricType == "annual_production" }
                val co2Metric = metrics.firstOrNull { it.metricType == "co2_savings" }
                val houseMetric = metrics.firstOrNull { it.metricType == "households_supplied" } ?: metrics.firstOrNull { it.metricType == "household_equivalent" }
                val muniMetric = metrics.firstOrNull { it.metricType == "municipal_participation" }
                val annualGwh = annualMetric?.value?.let { it / 1_000_000.0 }
                val co2Tons = co2Metric?.value?.let { it / 1000.0 }
                val households = houseMetric?.value
                val municipalBenefit = muniMetric?.value

                uiState = uiState.copy(
                    selectedPreviewData = EntityPreviewData(
                        id = park.id,
                        type = EntityType.PARK,
                        title = park.name,
                        subtitle = "Gemeinde ${park.municipalityName}",
                        statusLabel = parkStatuses[park.id] ?: "Aktiv",
                        turbineCountLabel = formatTurbineLabel(park.turbineCount),
                        capacityLabel = formatCapacityLabel(park.installedCapacityKw),
                        turbines = turbines.toPreviewPoints(),
                        annualProductionGwh = annualGwh,
                        co2SavingsTons = co2Tons,
                        householdsSupplied = households,
                        municipalBenefitEur = municipalBenefit,
                    )
                )
            } catch (e: Throwable) {
                // Preview data is supplemental; selection itself should remain responsive.
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
                val households = regionSummary?.householdEquivalent
                val municipalBenefit = regionSummary?.municipalBenefitEur
                
                val entityType = when (type.lowercase()) {
                    "city" -> EntityType.CITY
                    "district" -> EntityType.DISTRICT
                    "state" -> EntityType.STATE
                    else -> EntityType.CITY
                }
                
                val regionTypeLabel = when (entityType) {
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
                        statusLabel = regionTypeLabel,
                        turbineCountLabel = regionSummary?.turbineCount?.let { formatTurbineLabel(it) },
                        capacityLabel = regionSummary?.installedCapacityKw?.let { formatCapacityLabel(it) },
                        annualProductionGwh = annualProductionGwh,
                        co2SavingsTons = co2SavingsTons,
                        householdsSupplied = households,
                        municipalBenefitEur = municipalBenefit,
                    )
                )
            } catch (e: Throwable) {
                // Region preview metrics are supplemental.
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
            dataHintRepository.submitDataHint(
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

    fun startPinPlacement(reportPark: WindPark? = uiState.selectedPark) {
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
        if (uiState.previewSheetState != PreviewSheetState.Hidden) {
            uiState = uiState.copy(previewSheetState = PreviewSheetState.Peek)
        }
    }

    fun minimizePreview() {
        if (uiState.previewSheetState != PreviewSheetState.Hidden) {
            uiState = uiState.copy(previewSheetState = PreviewSheetState.Minimized)
        }
    }

    fun dismissPreview() {
        uiState = uiState.copy(
            selectedPark = null,
            selectedPreviewData = null,
            previewSheetState = PreviewSheetState.Hidden
        )
    }

    fun centerOnLocation(lat: Double, lon: Double) {
        clearViewportBounds()
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
        clearViewportBounds()
        uiState = uiState.copy(zoomLevel = zoom.coerceIn(5.0f, 18.0f))
        applyFilters()
    }

    fun onClusterClicked(lat: Double, lon: Double) {
        clearViewportBounds()
        uiState = uiState.copy(
            mapCenterLat = lat,
            mapCenterLon = lon,
            zoomLevel = (uiState.zoomLevel + 2.0f).coerceIn(5.0f, 18.0f),
            selectedPark = null,
            selectedPreviewData = null,
            previewSheetState = PreviewSheetState.Hidden,
        )
        applyFilters()
    }

    fun onMapMoved(lat: Double, lon: Double, zoom: Float) {
        clearViewportBounds()
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
        val nextZoom = zoom.coerceIn(5.0f, 18.0f)
        val nextBounds = MapBounds(
            swLat = minOf(swLat, neLat),
            swLon = swLon,
            neLat = maxOf(swLat, neLat),
            neLon = neLon,
        )
        val boundsChanged = viewportBounds?.isCloseTo(nextBounds) != true
        viewportBounds = nextBounds
        viewportBoundsZoom = nextZoom

        if (boundsChanged ||
            abs(uiState.mapCenterLat - lat) > 0.0001 ||
            abs(uiState.mapCenterLon - lon) > 0.0001 ||
            abs(uiState.zoomLevel - zoom) > 0.1
        ) {
            uiState = uiState.copy(
                mapCenterLat = lat,
                mapCenterLon = lon,
                zoomLevel = nextZoom
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
        val currentFilters = snapshot.filters
        val currentStatuses = parkStatuses
        val bounds = viewportBounds
        val markerBounds = bounds ?: fallbackBounds(snapshot.mapCenterLat, snapshot.mapCenterLon, snapshot.zoomLevel)

        filterJob = viewModelScope.launch {
            try {
                delay(150)
                val filteredParks = withContext(Dispatchers.Default) {
                    val rawParks = applyMapFilters(snapshot.parks, currentStatuses, currentFilters)
                    filterParksInBounds(
                        rawParks,
                        markerBounds,
                    )
                }

                val markers = if (snapshot.zoomLevel >= TurbineMarkerMinZoom) {
                    val turbines = repository.getWindTurbinesInBounds(
                        swLat = markerBounds.swLat,
                        swLon = markerBounds.swLon,
                        neLat = markerBounds.neLat,
                        neLon = markerBounds.neLon,
                    )
                    withContext(Dispatchers.Default) {
                        val filteredTurbines = filterTurbines(turbines, currentFilters)
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
                uiState = uiState.copy(
                    filteredParks = emptyList(),
                    mapMarkers = emptyList()
                )
            }
        }
    }

    private fun previewFocusedCenter(
        latitude: Double,
        longitude: Double,
        zoom: Float,
    ): Pair<Double, Double> {
        val latSpan = focusedVisibleLatitudeSpan(latitude, zoom)
        return latitude - (latSpan * SelectedParkFocusOffsetFraction) to longitude
    }

    private fun focusedVisibleLatitudeSpan(latitude: Double, zoom: Float): Double {
        val bounds = viewportBounds
        val boundsZoom = viewportBoundsZoom
        if (bounds != null && boundsZoom != null) {
            val currentSpan = bounds.neLat - bounds.swLat
            if (currentSpan > 0.0) {
                return (currentSpan * 2.0.pow((boundsZoom - zoom).toDouble()))
                    .coerceIn(0.003, 30.0)
            }
        }
        return fallbackVisibleLatitudeSpan(latitude, zoom)
    }

    private fun clearViewportBounds() {
        viewportBounds = null
        viewportBoundsZoom = null
    }

}

private fun formatTurbineLabel(count: Int): String =
    "$count Anlage${if (count == 1) "" else "n"}"

private fun formatCapacityLabel(capacityKw: Long?): String? {
    val capacity = capacityKw ?: return null
    val capacityMw = capacity / 1_000.0
    return "${formatGermanNumber(capacityMw, 1)} MW"
}

private fun List<WindTurbine>.toPreviewPoints(): List<PreviewTurbinePoint> =
    map { turbine ->
        PreviewTurbinePoint(
            id = turbine.id,
            latitude = turbine.latitude,
            longitude = turbine.longitude,
            statusLabel = turbine.status?.let(::formatStatusLabel),
            hubHeightM = turbine.hubHeightM,
            rotorDiameterM = turbine.rotorDiameterM,
            installedCapacityKw = turbine.installedCapacityKw,
            commissioningYear = turbine.commissioningYear,
        )
    }

private fun formatStatusLabel(status: String): String {
    val lower = status.lowercase()
    return when {
        lower.contains("bau") || lower.contains("errichtung") -> "Im Bau"
        lower.contains("stillgelegt") -> "Stillgelegt"
        lower.contains("geplant") -> "Geplant"
        lower.contains("betrieb") || lower.contains("aktiv") -> "Aktiv"
        else -> status
    }
}
