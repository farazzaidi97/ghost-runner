package com.ghostrunner.core.engine

import com.ghostrunner.core.domain.CadenceSample
import com.ghostrunner.core.domain.LocationSample
import com.ghostrunner.core.domain.PaceProfile
import com.ghostrunner.core.domain.PaceState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PacingEngineTest {

    private val targetProfile = PaceProfile(
        label = "5:00/km",
        targetMetersPerSec = 3.33f,
        strideLengthMeters = 1.0f,
    )

    private fun loc(speed: Float, accuracy: Float = 5f, ts: Long = 0L) =
        LocationSample(speed, accuracy, ts)

    @Test
    fun `delta positive when below target maps to Behind`() {
        val engine = PacingEngine(filter = MovingAverageFilter(1))
        val state = engine.onLocation(loc(2.0f), targetProfile)
        assertThat(state).isInstanceOf(PaceState.Behind::class.java)
        assertThat(state.deltaMetersPerSec).isWithin(1e-4f).of(targetProfile.targetMetersPerSec - 2.0f)
    }

    @Test
    fun `delta within epsilon maps to OnPace`() {
        val engine = PacingEngine(filter = MovingAverageFilter(1))
        val state = engine.onLocation(loc(targetProfile.targetMetersPerSec + 0.02f), targetProfile)
        assertThat(state).isInstanceOf(PaceState.OnPace::class.java)
    }

    @Test
    fun `delta negative when above target maps to Ahead`() {
        val engine = PacingEngine(filter = MovingAverageFilter(1))
        val state = engine.onLocation(loc(5.0f), targetProfile)
        assertThat(state).isInstanceOf(PaceState.Ahead::class.java)
    }

    @Test
    fun `state transitions across the on-pace boundary`() {
        val engine = PacingEngine(filter = MovingAverageFilter(1))
        // Start behind
        assertThat(engine.onLocation(loc(2.0f), targetProfile))
            .isInstanceOf(PaceState.Behind::class.java)
        // Cross to on-pace
        assertThat(engine.onLocation(loc(targetProfile.targetMetersPerSec), targetProfile))
            .isInstanceOf(PaceState.OnPace::class.java)
        // Cross to ahead
        assertThat(engine.onLocation(loc(4.5f), targetProfile))
            .isInstanceOf(PaceState.Ahead::class.java)
    }

    @Test
    fun `smoothing filter dampens isolated GPS spike`() {
        val engine = PacingEngine(filter = MovingAverageFilter(3))
        engine.onLocation(loc(3.3f), targetProfile)
        engine.onLocation(loc(3.4f), targetProfile)
        // A single spike to 9 m/s would normally indicate user is far Ahead,
        // but with a 3-sample average across [3.3, 3.4, 9.0] the smoothed
        // value is (3.3+3.4+9.0)/3 = 5.23 — still Ahead, but delta is much
        // smaller than the spike implies.
        val state = engine.onLocation(loc(9.0f), targetProfile)
        val smoothedSpeed = targetProfile.targetMetersPerSec - state.deltaMetersPerSec
        assertThat(smoothedSpeed).isWithin(1e-3f).of((3.3f + 3.4f + 9.0f) / 3f)
    }

    @Test
    fun `falls back to cadence times stride when GPS accuracy poor`() {
        val engine = PacingEngine(filter = MovingAverageFilter(1))
        engine.onCadence(CadenceSample(stepsPerMinute = 180f, timestampMillis = 0L))
        // GPS says 0.5 m/s but accuracy is 30m (untrustable);
        // fallback should use cadence (180/60 = 3 spm * 1.0m stride = 3 m/s).
        val state = engine.onLocation(loc(speed = 0.5f, accuracy = 30f), targetProfile)
        val effectiveSpeed = targetProfile.targetMetersPerSec - state.deltaMetersPerSec
        assertThat(effectiveSpeed).isWithin(1e-3f).of(3.0f)
    }

    @Test
    fun `falls back through to GPS speed when accuracy poor and no cadence yet`() {
        val engine = PacingEngine(filter = MovingAverageFilter(1))
        val state = engine.onLocation(loc(speed = 0.5f, accuracy = 30f), targetProfile)
        val effectiveSpeed = targetProfile.targetMetersPerSec - state.deltaMetersPerSec
        assertThat(effectiveSpeed).isWithin(1e-3f).of(0.5f)
    }

    @Test
    fun `reset clears filter and cadence between sessions`() {
        val engine = PacingEngine(filter = MovingAverageFilter(3))
        engine.onCadence(CadenceSample(stepsPerMinute = 180f, timestampMillis = 0L))
        engine.onLocation(loc(2.0f), targetProfile)
        engine.onLocation(loc(2.0f), targetProfile)
        engine.reset()

        // After reset, a single sample should average to itself (no priming).
        val state = engine.onLocation(loc(targetProfile.targetMetersPerSec), targetProfile)
        assertThat(state).isInstanceOf(PaceState.OnPace::class.java)
    }
}
