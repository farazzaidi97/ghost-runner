package com.ghostrunner.core.audio

import java.util.Random
import kotlin.math.exp

class FootstepSynthesizer(
    val sampleRateHz: Int = DEFAULT_SAMPLE_RATE,
    val durationMs: Int = DEFAULT_DURATION_MS,
    seed: Long = STABLE_SEED,
) {
    private val random = Random(seed)

    fun synthesize(): ShortArray {
        val sampleCount = (sampleRateHz * durationMs) / 1000
        val out = ShortArray(sampleCount)

        var lowpass = 0f
        for (i in 0 until sampleCount) {
            val white = random.nextFloat() * 2f - 1f
            lowpass += LOWPASS_COEFF * (white - lowpass)

            val t = i.toFloat() / sampleCount
            val envelope = exp(-DECAY_STRENGTH * t)

            val v = (lowpass * envelope * HEADROOM).coerceIn(-1f, 1f)
            out[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE: Int = 44_100
        const val DEFAULT_DURATION_MS: Int = 80
        private const val STABLE_SEED: Long = 42L
        private const val LOWPASS_COEFF: Float = 0.25f
        private const val DECAY_STRENGTH: Float = 5f
        private const val HEADROOM: Float = 0.7f
    }
}
