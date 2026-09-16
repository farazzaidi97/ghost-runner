package com.ghostrunner.core.domain

data class CadenceSample(
    val stepsPerMinute: Float,
    val timestampMillis: Long,
) {
    fun toMetersPerSec(strideMeters: Float): Float = (stepsPerMinute / 60f) * strideMeters
}
