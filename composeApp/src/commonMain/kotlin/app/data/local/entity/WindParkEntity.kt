package app.data.local.entity

data class WindParkEntity(
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
    val turbineCount: Int?,
    val installedCapacityKw: Long?,
    val groupingMethod: String,
    val sourceName: String,
    val sourceUrl: String,
    val sourceUpdatedAt: String,
    val dataQuality: String,
)

