package com.fieldbook.shared.screens.collect.traits

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.fieldbook.shared.AndroidAppContextHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun captureCurrentTraitLocation(): LocationCaptureResult {
    val context = AndroidAppContextHolder.context
    val locationManager = context.getSystemService(LocationManager::class.java)
        ?: return LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE)

    val hasFinePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarsePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFinePermission && !hasCoarsePermission) {
        return LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE)
    }

    val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    if (!gpsEnabled && !networkEnabled) {
        return LocationCaptureResult(failure = LocationCaptureFailure.SETTINGS_REQUIRED)
    }

    val provider = when {
        gpsEnabled && hasFinePermission -> LocationManager.GPS_PROVIDER
        networkEnabled -> LocationManager.NETWORK_PROVIDER
        gpsEnabled -> LocationManager.GPS_PROVIDER
        else -> null
    } ?: return LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return suspendCancellableCoroutine { continuation ->
            try {
                locationManager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                    if (!continuation.isActive) return@getCurrentLocation

                    continuation.resume(
                        if (location != null) {
                            LocationCaptureResult(
                                location = CapturedLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                )
                            )
                        } else {
                            legacyLastKnownLocation(locationManager, provider)
                        }
                    )
                }
            } catch (_: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(legacyLastKnownLocation(locationManager, provider))
                }
            }
        }
    }

    return legacyLastKnownLocation(locationManager, provider)
}

actual fun openTraitLocationSettings() {
    val context = AndroidAppContextHolder.context
    context.startActivity(
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun legacyLastKnownLocation(
    locationManager: LocationManager,
    provider: String,
): LocationCaptureResult {
    return try {
        val location = locationManager.getLastKnownLocation(provider)
        if (location != null) {
            LocationCaptureResult(
                location = CapturedLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            )
        } else {
            LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE)
        }
    } catch (_: Throwable) {
        LocationCaptureResult(failure = LocationCaptureFailure.UNAVAILABLE)
    }
}
