@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.fieldbook.shared.screens.collect.traits

import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

private var activeLocationDelegate: SingleLocationCaptureDelegate? = null

actual suspend fun captureCurrentTraitLocation(): LocationCaptureResult {
    return suspendCancellableCoroutine { continuation ->
        val manager = CLLocationManager()
        manager.desiredAccuracy = kCLLocationAccuracyBest

        val delegate = SingleLocationCaptureDelegate(
            manager = manager,
            onResult = { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
                activeLocationDelegate = null
            }
        )

        activeLocationDelegate = delegate
        manager.delegate = delegate
        delegate.begin()

        continuation.invokeOnCancellation {
            manager.stopUpdatingLocation()
            manager.delegate = null
            activeLocationDelegate = null
        }
    }
}

actual fun openTraitLocationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url)
}

private class SingleLocationCaptureDelegate(
    private val manager: CLLocationManager,
    private val onResult: (LocationCaptureResult) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    private var finished = false

    fun begin() {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> requestLocation()

            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> finish(
                LocationCaptureResult(failure = LocationCaptureFailure.SETTINGS_REQUIRED)
            )

            else -> finish(LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE))
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> requestLocation()

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> finish(
                LocationCaptureResult(failure = LocationCaptureFailure.SETTINGS_REQUIRED)
            )
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location == null) {
            finish(LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE))
            return
        }

        finish(
            LocationCaptureResult(
                location = location.toCapturedLocation()
            )
        )
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError
    ) {
        finish(LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE))
    }

    private fun requestLocation() {
        manager.requestLocation()
    }

    private fun finish(result: LocationCaptureResult) {
        if (finished) return
        finished = true
        manager.stopUpdatingLocation()
        manager.delegate = null
        onResult(result)
    }
}

private fun CLLocation.toCapturedLocation(): CapturedLocation {
    return coordinate.useContents {
        CapturedLocation(
            latitude = latitude,
            longitude = longitude,
        )
    }
}
