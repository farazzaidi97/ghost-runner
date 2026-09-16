package com.ghostrunner.app.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.ghostrunner.core.domain.CadenceSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class StepDetectorSource(context: Context) {

    private val sensorManager: SensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    val isSupported: Boolean get() = sensor != null

    fun samples(windowMillis: Long = DEFAULT_WINDOW_MILLIS): Flow<CadenceSample> = callbackFlow {
        val currentSensor = sensor
        if (currentSensor == null) {
            awaitClose { }
            return@callbackFlow
        }

        val recentSteps = ArrayDeque<Long>()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                recentSteps.addLast(now)
                while (recentSteps.isNotEmpty() && recentSteps.first() < now - windowMillis) {
                    recentSteps.removeFirst()
                }
                val minutes = windowMillis / 60_000f
                val spm = recentSteps.size / minutes
                trySend(CadenceSample(stepsPerMinute = spm, timestampMillis = now))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, currentSensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS: Long = 10_000L
    }
}
