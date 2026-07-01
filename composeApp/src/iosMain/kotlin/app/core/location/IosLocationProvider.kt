package app.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {
    private val locationManager = CLLocationManager()

    override fun hasPermission(): Boolean {
        val status = locationManager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse || 
               status == kCLAuthorizationStatusAuthorizedAlways
    }

    override suspend fun getCurrentLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        val status = locationManager.authorizationStatus
        if (status == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
        }

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? platform.CoreLocation.CLLocation
                manager.stopUpdatingLocation()
                if (location != null) {
                    val lat = location.coordinate.useContents { latitude }
                    val lon = location.coordinate.useContents { longitude }
                    if (continuation.isActive) {
                        continuation.resume(lat to lon)
                    }
                } else {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
                manager.stopUpdatingLocation()
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                val newStatus = manager.authorizationStatus
                if (newStatus == kCLAuthorizationStatusAuthorizedWhenInUse || 
                    newStatus == kCLAuthorizationStatusAuthorizedAlways) {
                    manager.startUpdatingLocation()
                } else if (newStatus != kCLAuthorizationStatusNotDetermined) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }

        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest

        if (hasPermission()) {
            locationManager.startUpdatingLocation()
        } else {
            locationManager.requestWhenInUseAuthorization()
        }

        continuation.invokeOnCancellation {
            locationManager.stopUpdatingLocation()
        }
    }
}
