package com.ghostrunner.core.audio

import com.ghostrunner.core.domain.PaceState
import kotlin.math.abs

/**
 * 7.1 channel layout indices (Android CHANNEL_OUT_7POINT1 interleave order):
 *   0 FL, 1 FR, 2 FC, 3 LFE, 4 BL, 5 BR, 6 SL, 7 SR.
 */
class ChannelPanCalculator {

    fun gainsFor71(state: PaceState): FloatArray {
        val proximity = proximityFromDelta(state.deltaMetersPerSec)
        return when (state) {
            is PaceState.Behind -> floatArrayOf(
                /* FL */ 0.10f, /* FR */ 0.10f,
                /* FC */ 0f, /* LFE */ 0f,
                /* BL */ proximity, /* BR */ proximity,
                /* SL */ proximity * 0.5f, /* SR */ proximity * 0.5f,
            )
            is PaceState.OnPace -> floatArrayOf(
                0.20f, 0.20f,
                0f, 0f,
                0.20f, 0.20f,
                0.60f, 0.60f,
            )
            is PaceState.Ahead -> floatArrayOf(
                /* FL */ proximity, /* FR */ proximity,
                /* FC */ proximity * 0.6f, /* LFE */ 0f,
                /* BL */ 0.05f, /* BR */ 0.05f,
                /* SL */ proximity * 0.3f, /* SR */ proximity * 0.3f,
            )
        }
    }

    fun gainsForStereo(state: PaceState): FloatArray {
        val base = proximityFromDelta(state.deltaMetersPerSec)
        return when (state) {
            is PaceState.Behind -> floatArrayOf(base, base)
            is PaceState.OnPace -> floatArrayOf(0.7f, 0.7f)
            is PaceState.Ahead -> floatArrayOf(base * 0.4f, base * 0.4f)
        }
    }

    private fun proximityFromDelta(deltaMps: Float): Float {
        val normalized = (abs(deltaMps) / MAX_DELTA_MPS).coerceIn(0f, 1f)
        return (1f - normalized).coerceIn(MIN_GAIN, MAX_GAIN)
    }

    companion object {
        const val MAX_DELTA_MPS: Float = 2.0f
        const val MIN_GAIN: Float = 0.05f
        const val MAX_GAIN: Float = 1.0f
    }
}
