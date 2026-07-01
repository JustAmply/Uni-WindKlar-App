package app.core.location

interface LocationProvider {
    fun hasPermission(): Boolean
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}
