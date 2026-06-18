package app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.core.model.WindPark

@Composable
expect fun PlatformMapView(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    parks: List<WindPark>,
    selectedParkId: String?,
    onMapMoved: (lat: Double, lon: Double, zoom: Float) -> Unit,
    onParkClicked: (String) -> Unit,
    modifier: Modifier = Modifier
)
