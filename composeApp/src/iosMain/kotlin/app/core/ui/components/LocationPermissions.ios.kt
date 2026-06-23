package app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val manager = remember { CLLocationManager() }
    val delegate = remember { LocationPermissionDelegate() }

    delegate.onPermissionResult = { granted ->
        currentOnResult.value(granted)
    }

    DisposableEffect(manager, delegate) {
        manager.delegate = delegate
        onDispose {
            if (manager.delegate === delegate) {
                manager.delegate = null
            }
        }
    }

    return remember(manager, delegate) {
        {
            val status = manager.authorizationStatus
            when {
                status.isAuthorized() -> delegate.report(status)
                status == kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
                else -> delegate.report(status)
            }
        }
    }
}

private class LocationPermissionDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    var onPermissionResult: (Boolean) -> Unit = {}

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        report(manager.authorizationStatus)
    }

    fun report(status: CLAuthorizationStatus) {
        if (status != kCLAuthorizationStatusNotDetermined) {
            onPermissionResult(status.isAuthorized())
        }
    }
}

private fun CLAuthorizationStatus.isAuthorized(): Boolean =
    this == kCLAuthorizationStatusAuthorizedWhenInUse || this == kCLAuthorizationStatusAuthorizedAlways
