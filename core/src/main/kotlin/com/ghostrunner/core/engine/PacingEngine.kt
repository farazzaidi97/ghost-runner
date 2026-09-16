package com.ghostrunner.core.engine

import com.ghostrunner.core.domain.CadenceSample
import com.ghostrunner.core.domain.LocationSample
import com.ghostrunner.core.domain.PaceProfile
import com.ghostrunner.core.domain.PaceState

class PacingEngine(
    private val filter: MovingAverageFilter = MovingAverageFilter(),
    private val inertialFallback: InertialFallback = InertialFallback(),
) {

    private var lastCadence: CadenceSample? = null
    private var lastState: PaceState = PaceState.OnPace(0f)

    val state: PaceState get() = lastState

    fun onCadence(sample: CadenceSample) {
        lastCadence = sample
    }

    fun onLocation(sample: LocationSample, profile: PaceProfile): PaceState {
        val rawSpeedMps = if (inertialFallback.shouldFallback(sample)) {
            lastCadence?.let { inertialFallback.estimateSpeed(it, profile.strideLengthMeters) }
                ?: sample.speedMetersPerSec
        } else {
            sample.speedMetersPerSec
        }

        val smoothed = filter.add(rawSpeedMps)
        val delta = profile.targetMetersPerSec - smoothed
        return PaceState.fromDelta(delta).also { lastState = it }
    }

    fun reset() {
        filter.reset()
        lastCadence = null
        lastState = PaceState.OnPace(0f)
    }
}
