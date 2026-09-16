package com.ghostrunner.core.domain

data class PaceProfile(
    val id: Long = 0L,
    val label: String,
    val targetMetersPerSec: Float,
    val strideLengthMeters: Float = DEFAULT_STRIDE_METERS,
    val isDefault: Boolean = false,
) {
    companion object {
        const val DEFAULT_STRIDE_METERS: Float = 1.0f

        fun fromPaceMinKm(label: String, paceMinPerKm: Float, strideMeters: Float = DEFAULT_STRIDE_METERS): PaceProfile {
            val mps = 1000f / (paceMinPerKm * 60f)
            return PaceProfile(label = label, targetMetersPerSec = mps, strideLengthMeters = strideMeters)
        }
    }
}
