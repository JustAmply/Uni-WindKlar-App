package app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationProvider(private val context: Context) : LocationProvider {

    override fun hasPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        if (!hasPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        // Try to get last known location first for speed
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }

        // If we have a recent/accurate last known location (e.g. newer than 60s), use it
        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 60_000) {
            continuation.resume(bestLocation.latitude to bestLocation.longitude)
            return@suspendCancellableCoroutine
        }

        // Otherwise, request a fresh location update
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (continuation.isActive) {
                    continuation.resume(location.latitude to location.longitude)
                }
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider != null) {
            try {
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(bestLocation?.let { it.latitude to it.longitude })
                }
            }
        } else {
            // Fallback to last known if available, else null
            continuation.resume(bestLocation?.let { it.latitude to it.longitude })
        }
    }
}
