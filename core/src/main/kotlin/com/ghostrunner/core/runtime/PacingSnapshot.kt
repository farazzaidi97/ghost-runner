package com.ghostrunner.core.runtime

import com.ghostrunner.core.domain.PaceProfile
import com.ghostrunner.core.domain.PaceState

data class PacingSnapshot(
    val profile: PaceProfile,
    val state: PaceState,
    val currentSpeedMps: Float,
    val accuracyMeters: Float,
    val stepsPerMinute: Float?,
    val isStereoFallback: Boolean,
) {
    val targetSpeedMps: Float get() = profile.targetMetersPerSec
}
