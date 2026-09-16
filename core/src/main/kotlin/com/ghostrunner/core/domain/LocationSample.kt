package com.ghostrunner.core.domain

data class LocationSample(
    val speedMetersPerSec: Float,
    val accuracyMeters: Float,
    val timestampMillis: Long,
) {
    val hasReliableFix: Boolean get() = accuracyMeters in 0f..MAX_TRUSTED_ACCURACY_METERS

    companion object {
        const val MAX_TRUSTED_ACCURACY_METERS: Float = 15.0f
    }
}
