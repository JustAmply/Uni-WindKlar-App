package app.core.model

enum class MapMarkerKind {
    Park,
    Cluster,
    Turbine,
    PlacementPin,
}

data class MapMarkerUiModel(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val kind: MapMarkerKind,
    val count: Int,
    val parkId: String? = null,
)
