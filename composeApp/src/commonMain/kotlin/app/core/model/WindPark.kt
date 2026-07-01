package app.core.model

data class WindPark(
    val id: String,
    val name: String,
    val municipalityId: String,
    val municipalityName: String,
    val districtId: String,
    val districtName: String,
    val stateId: String,
    val stateName: String,
    val latitude: Double,
    val longitude: Double,
    val turbineCount: Int,
    val installedCapacityKw: Long?,
    val isFavorite: Boolean = false,
    val sourceName: String = "",
    val sourceUrl: String = "",
    val sourceUpdatedAt: String = "",
    val dataQuality: String = "",
) {
    val municipality: String get() = municipalityName

    val xOffset: Double = (longitude + 180.0) / 360.0
    val yOffset: Double = run {
        val latitudeRadians = latitude.coerceIn(-85.05112878, 85.05112878) * kotlin.math.PI / 180.0
        val sinLatitude = kotlin.math.sin(latitudeRadians)
        0.5 - kotlin.math.ln((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * kotlin.math.PI)
    }
}

