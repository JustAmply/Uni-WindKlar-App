package app.feature.map

import app.core.model.WindPark
import app.core.model.Metric

enum class ParkPreviewSheetState {
    Expanded,
    Minimized,
}

data class MapUiState(
    val isLoading: Boolean = false,
    val parks: List<WindPark> = emptyList(),
    val filteredParks: List<WindPark> = emptyList(),
    val selectedPark: WindPark? = null,
    val previewSheetState: ParkPreviewSheetState = ParkPreviewSheetState.Expanded,
    val selectedParkMetrics: List<Metric> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<WindPark> = emptyList(),
    val showSearchOverlay: Boolean = false,
    val mapCenterLat: Double = 51.1657, // Default center of Germany
    val mapCenterLon: Double = 10.4515,
    val zoomLevel: Float = 6.0f,
    val selectedStatus: String = "Aktiv", // "Aktiv", "Geplant", "Im Bau", "Stillgelegt"
)
