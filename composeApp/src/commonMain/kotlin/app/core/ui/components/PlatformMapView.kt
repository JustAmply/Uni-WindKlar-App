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
    onParkClicked: (String) -> Unit,
    onClusterClicked: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier
)
