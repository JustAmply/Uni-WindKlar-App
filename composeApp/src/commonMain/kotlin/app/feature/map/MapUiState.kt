package app.feature.map

import app.core.model.MapMarkerUiModel
import app.core.model.WindPark
import app.core.ui.components.EntityPreviewData
import app.core.ui.components.PreviewSheetState

sealed interface MapSearchResult {
    data class State(
        val id: String,
        val name: String,
        val latitude: Double,
        val longitude: Double
    ) : MapSearchResult

    data class District(
        val id: String,
        val name: String,
        val stateName: String,
        val latitude: Double,
        val longitude: Double
    ) : MapSearchResult

    data class Municipality(
        val id: String,
        val name: String,
        val districtName: String,
        val stateName: String,
        val latitude: Double,
        val longitude: Double
    ) : MapSearchResult

    data class Park(
        val park: WindPark
    ) : MapSearchResult
}

data class MapUiState(
    val isLoading: Boolean = false,
    val parks: List<WindPark> = emptyList(),
    val filteredParks: List<WindPark> = emptyList(),
    val mapMarkers: List<MapMarkerUiModel> = emptyList(),
    val selectedPark: WindPark? = null,
    val selectedPreviewData: EntityPreviewData? = null,
    val previewSheetState: PreviewSheetState = PreviewSheetState.Expanded,
    val searchQuery: String = "",
    val searchResults: List<MapSearchResult> = emptyList(),
    val showSearchOverlay: Boolean = false,
    val mapCenterLat: Double = 51.1657, // Default center of Germany
    val mapCenterLon: Double = 10.4515,
    val zoomLevel: Float = 6.0f,
    val selectedStatus: String = "Alle",
    val isPinPlacementMode: Boolean = false,
    val placementMarkerLat: Double = 0.0,
    val placementMarkerLon: Double = 0.0,
)

