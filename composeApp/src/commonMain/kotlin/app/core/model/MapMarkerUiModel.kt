package app.core.model

enum class MapMarkerKind {
    Park,
    Cluster,
}

data class MapMarkerUiModel(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val kind: MapMarkerKind,
    val count: Int,
    val parkId: String? = null,
)
