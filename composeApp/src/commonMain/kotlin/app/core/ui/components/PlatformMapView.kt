package app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.core.model.MapMarkerUiModel

@Composable
expect fun PlatformMapView(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    markers: List<MapMarkerUiModel>,
    selectedParkId: String?,
    onMapMoved: (lat: Double, lon: Double, zoom: Float) -> Unit,
    onMapMovedWithBounds: ((
        lat: Double,
        lon: Double,
        zoom: Float,
        swLat: Double,
        swLon: Double,
        neLat: Double,
        neLon: Double,
    ) -> Unit)? = null,
    onParkClicked: (String) -> Unit,
    onClusterClicked: (lat: Double, lon: Double) -> Unit,
    onPlacementPinDragged: ((lat: Double, lon: Double) -> Unit)? = null,
    modifier: Modifier = Modifier
)
