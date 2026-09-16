package com.ghostrunner.core.domain

sealed class PaceState {
    abstract val deltaMetersPerSec: Float

    data class Behind(override val deltaMetersPerSec: Float) : PaceState()
    data class OnPace(override val deltaMetersPerSec: Float) : PaceState()
    data class Ahead(override val deltaMetersPerSec: Float) : PaceState()

    companion object {
        const val ON_PACE_EPSILON_MPS: Float = 0.05f

        fun fromDelta(deltaMps: Float): PaceState = when {
            deltaMps > ON_PACE_EPSILON_MPS -> Behind(deltaMps)
            deltaMps < -ON_PACE_EPSILON_MPS -> Ahead(deltaMps)
            else -> OnPace(deltaMps)
        }
    }
}
