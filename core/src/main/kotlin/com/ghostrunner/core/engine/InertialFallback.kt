package com.ghostrunner.core.engine

import com.ghostrunner.core.domain.CadenceSample
import com.ghostrunner.core.domain.LocationSample

class InertialFallback {

    fun shouldFallback(sample: LocationSample): Boolean = !sample.hasReliableFix

    fun estimateSpeed(cadence: CadenceSample, strideMeters: Float): Float =
        cadence.toMetersPerSec(strideMeters)
}
