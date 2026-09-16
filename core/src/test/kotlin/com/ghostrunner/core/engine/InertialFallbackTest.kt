package com.ghostrunner.core.engine

import com.ghostrunner.core.domain.CadenceSample
import com.ghostrunner.core.domain.LocationSample
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InertialFallbackTest {

    private val fallback = InertialFallback()

    @Test
    fun `does not fall back when accuracy is at or below trusted threshold`() {
        val sample = LocationSample(
            speedMetersPerSec = 3.0f,
            accuracyMeters = 15.0f,
            timestampMillis = 0L,
        )
        assertThat(fallback.shouldFallback(sample)).isFalse()
    }

    @Test
    fun `falls back when accuracy exceeds trusted threshold`() {
        val sample = LocationSample(
            speedMetersPerSec = 3.0f,
            accuracyMeters = 15.5f,
            timestampMillis = 0L,
        )
        assertThat(fallback.shouldFallback(sample)).isTrue()
    }

    @Test
    fun `falls back when accuracy is negative or unreliable sentinel`() {
        val sample = LocationSample(
            speedMetersPerSec = 3.0f,
            accuracyMeters = -1.0f,
            timestampMillis = 0L,
        )
        assertThat(fallback.shouldFallback(sample)).isTrue()
    }

    @Test
    fun `estimateSpeed equals cadence times stride normalised to per-second`() {
        // 180 spm × 1.2 m stride = 3.6 m/s
        val cadence = CadenceSample(stepsPerMinute = 180f, timestampMillis = 0L)
        assertThat(fallback.estimateSpeed(cadence, strideMeters = 1.2f))
            .isWithin(1e-4f).of(3.6f)
    }

    @Test
    fun `estimateSpeed scales linearly with cadence`() {
        val low = CadenceSample(stepsPerMinute = 60f, timestampMillis = 0L)
        val high = CadenceSample(stepsPerMinute = 120f, timestampMillis = 0L)
        val stride = 1.0f
        assertThat(fallback.estimateSpeed(high, stride))
            .isWithin(1e-4f).of(fallback.estimateSpeed(low, stride) * 2f)
    }
}
