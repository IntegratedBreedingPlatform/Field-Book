package com.fieldbook.shared.screens.collect.traits

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
)

enum class LocationCaptureFailure {
    SETTINGS_REQUIRED,
    UNAVAILABLE,
}

data class LocationCaptureResult(
    val location: CapturedLocation? = null,
    val failure: LocationCaptureFailure? = null,
)

expect suspend fun captureCurrentTraitLocation(): LocationCaptureResult

expect fun openTraitLocationSettings()
