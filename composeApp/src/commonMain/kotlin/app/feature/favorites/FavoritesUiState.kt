package app.feature.favorites

data class FavoritesUiState(
    val parks: List<FavoriteParkUiModel> = emptyList(),
    val recents: List<FavoriteParkUiModel> = emptyList(),
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

enum class FavoriteParkThumbnail {
    Nordsee,
    Ostsee,
    Alpen,
}
