package app.feature.favorites

data class FavoritesUiState(
    val parks: List<FavoriteParkUiModel> = emptyList(),
    val regions: List<FavoriteRegionUiModel> = emptyList(),
    val recents: List<FavoriteParkUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
)


data class FavoriteParkUiModel(
    val id: String,
    val name: String,
    val distance: String,
    val production: String,
    val co2Reduction: String,
    val thumbnail: FavoriteParkThumbnail,
    val isFavorite: Boolean,
)

data class FavoriteRegionUiModel(
    val id: String,
    val name: String,
    val type: String, // "city", "district", "state"
    val typeLabel: String, // "Gemeinde", "Landkreis", "Bundesland"
    val production: String,
    val co2Reduction: String,
    val thumbnail: FavoriteParkThumbnail,
    val isFavorite: Boolean,
)

enum class FavoriteParkThumbnail {
    Nordsee,
    Ostsee,
    Alpen,
    Feld,
    Waldkante,
    Herbst,
    Winter,
    Dorf,
}
