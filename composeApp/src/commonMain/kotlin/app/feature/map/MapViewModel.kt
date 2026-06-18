package app.feature.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.WindPark
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch

class MapViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState by mutableStateOf(MapUiState(isLoading = true))
        private set

    private var parkStatuses: Map<String, String> = emptyMap()

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


    private fun determineParkStatus(statuses: List<String>): String {
        if (statuses.isEmpty()) return "Aktiv"
        val lowerStatuses = statuses.map { it.lowercase() }
        if (lowerStatuses.any { it.contains("bau") || it.contains("errichtung") }) {
            return "Im Bau"
        }
        if (lowerStatuses.any { it.contains("betrieb") || it.contains("aktiv") }) {
            return "Aktiv"
        }
        if (lowerStatuses.any { it.contains("stillgelegt") }) {
            return "Stillgelegt"
        }
        return "Geplant"
    }

    fun setStatusFilter(status: String) {
        uiState = uiState.copy(selectedStatus = status)
        applyFilters()
    }

    fun onQueryChange(newQuery: String) {
        uiState = uiState.copy(searchQuery = newQuery)
        if (newQuery.length >= 2) {
            viewModelScope.launch {
                val results = repository.searchWindParks(newQuery)
                uiState = uiState.copy(
                    searchResults = results,
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

    fun onSearchResultSelected(park: WindPark) {
        val parkStatus = statusForPark(park.id)
        uiState = uiState.copy(
            mapCenterLat = park.latitude,
            mapCenterLon = park.longitude,
            zoomLevel = 12.0f,
            selectedPark = park,
            selectedStatus = parkStatus,
            filteredParks = parksForStatus(parkStatus),
            showSearchOverlay = false,
            searchQuery = ""
        )
        viewModelScope.launch {
            repository.recordRecentWindPark(park.id)
            val metrics = repository.getMetricsForPark(park.id)
            uiState = uiState.copy(selectedParkMetrics = metrics)
        }
    }

    fun onParkClicked(park: WindPark) {
        uiState = uiState.copy(selectedPark = park)
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



    fun dismissPreview() {
        uiState = uiState.copy(selectedPark = null)
    }

    fun centerOnLocation(lat: Double, lon: Double) {
        uiState = uiState.copy(
            mapCenterLat = lat,
            mapCenterLon = lon,
            zoomLevel = 10.0f
        )
    }

    fun onZoomChanged(zoom: Float) {
        uiState = uiState.copy(zoomLevel = zoom.coerceIn(5.0f, 18.0f))
    }

    fun onMapMoved(lat: Double, lon: Double, zoom: Float) {
        if (Math.abs(uiState.mapCenterLat - lat) > 0.0001 || 
            Math.abs(uiState.mapCenterLon - lon) > 0.0001 || 
            Math.abs(uiState.zoomLevel - zoom) > 0.1) {
            uiState = uiState.copy(
                mapCenterLat = lat,
                mapCenterLon = lon,
                zoomLevel = zoom.coerceIn(5.0f, 18.0f)
            )
        }
    }

    fun onPanChanged(deltaLat: Double, deltaLon: Double) {
        uiState = uiState.copy(
            mapCenterLat = uiState.mapCenterLat + deltaLat,
            mapCenterLon = uiState.mapCenterLon + deltaLon
        )
    }

    private fun applyFilters() {
        val currentStatus = uiState.selectedStatus
        uiState = uiState.copy(filteredParks = parksForStatus(currentStatus))
    }

    private fun parksForStatus(status: String): List<WindPark> =
        uiState.parks.filter { park -> statusForPark(park.id) == status }

    private fun statusForPark(parkId: String): String =
        parkStatuses[parkId] ?: "Aktiv"
}
