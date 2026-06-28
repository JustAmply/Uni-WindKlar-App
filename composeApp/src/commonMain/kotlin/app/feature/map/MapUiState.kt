package app.feature.map

import app.core.model.MapMarkerUiModel
import app.core.model.WindPark
import app.core.ui.components.EntityPreviewData
import app.core.ui.components.PreviewSheetState

sealed interface MapSearchResult {
    val key: String
        get() = when (this) {
            is State -> "state_$id"
            is District -> "district_$id"
            is Municipality -> "municipality_$id"
            is Park -> "park_${park.id}"
        }

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
    val previewSheetState: PreviewSheetState = PreviewSheetState.Hidden,
    val searchQuery: String = "",
    val searchResults: List<MapSearchResult> = emptyList(),
    val showSearchOverlay: Boolean = false,
    val parkStatuses: Map<String, String> = emptyMap(),
    val mapCenterLat: Double = 51.1657, // Default center of Germany
    val mapCenterLon: Double = 10.4515,
    val zoomLevel: Float = 6.0f,
    val filters: MapFilterState = MapFilterState(),
    val isPinPlacementMode: Boolean = false,
    val placementMarkerLat: Double = 0.0,
    val placementMarkerLon: Double = 0.0,
    val pendingReportPark: WindPark? = null,
    val recentParks: List<WindPark> = emptyList(),
    val isSearchFocused: Boolean = false,
)

data class MapFilterState(
    val status: MapStatusFilter = MapStatusFilter.All,
    val includeDecommissioned: Boolean = false,
    val sizeRange: MapParkSizeRange = MapParkSizeRange.All,
    val capacityRange: MapCapacityRange = MapCapacityRange.All,
) {
    val activeFilterCount: Int
        get() = listOf(
            status != MapStatusFilter.All,
            includeDecommissioned,
            sizeRange != MapParkSizeRange.All,
            capacityRange != MapCapacityRange.All,
        ).count { it }
}

enum class MapStatusFilter(val label: String) {
    All("Alle"),
    Active("Aktiv"),
    Planned("Geplant"),
}

enum class MapParkSizeRange(val label: String) {
    All("Alle"),
    Single("1 Anlage"),
    Small("2-5 Anlagen"),
    Large("6+ Anlagen");

    fun matches(turbineCount: Int): Boolean =
        when (this) {
            All -> true
            Single -> turbineCount == 1
            Small -> turbineCount in 2..5
            Large -> turbineCount >= 6
        }
}

enum class MapCapacityRange(val label: String) {
    All("Alle"),
    UpTo10Mw("bis 10 MW"),
    From10To50Mw("10-50 MW"),
    From50Mw("50+ MW");

    fun matches(installedCapacityKw: Long?): Boolean {
        val capacity = installedCapacityKw ?: return this == All
        return when (this) {
            All -> true
            UpTo10Mw -> capacity <= 10_000L
            From10To50Mw -> capacity > 10_000L && capacity <= 50_000L
            From50Mw -> capacity > 50_000L
        }
    }
}
