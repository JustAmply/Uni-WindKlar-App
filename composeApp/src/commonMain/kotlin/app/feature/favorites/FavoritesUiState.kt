package app.feature.favorites

data class FavoritesUiState(
    val parks: List<FavoriteParkUiModel> = listOf(
        FavoriteParkUiModel(
            id = "windpark-nordsee",
            name = "Windpark Nordsee",
            distance = "12 km entfernt",
            production = "42 GWh",
            co2Reduction = "25.000 t",
            thumbnail = FavoriteParkThumbnail.Nordsee,
        ),
        FavoriteParkUiModel(
            id = "windpark-ostsee",
            name = "Windpark Ostsee",
            distance = "24 km entfernt",
            production = "58 GWh",
            co2Reduction = "34.000 t",
            thumbnail = FavoriteParkThumbnail.Ostsee,
        ),
        FavoriteParkUiModel(
            id = "windpark-alpen",
            name = "Windpark Alpen",
            distance = "45 km entfernt",
            production = "36 GWh",
            co2Reduction = "21.000 t",
            thumbnail = FavoriteParkThumbnail.Alpen,
        ),
    ),
)

data class FavoriteParkUiModel(
    val id: String,
    val name: String,
    val distance: String,
    val production: String,
    val co2Reduction: String,
    val thumbnail: FavoriteParkThumbnail,
)

enum class FavoriteParkThumbnail {
    Nordsee,
    Ostsee,
    Alpen,
}
