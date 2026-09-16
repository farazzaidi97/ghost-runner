package com.ghostrunner.wear.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.ghostrunner.core.domain.CadenceSample
import com.ghostrunner.core.domain.LocationSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class HealthSample {
    data class Location(val sample: LocationSample) : HealthSample()
    data class Cadence(val sample: CadenceSample) : HealthSample()
}

class HealthServicesPipeline(context: Context) {

    private val exerciseClient: ExerciseClient =
        HealthServices.getClient(context.applicationContext).exerciseClient

    @SuppressLint("MissingPermission")
    fun samples(): Flow<HealthSample> = callbackFlow {
        val callback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                val now = System.currentTimeMillis()
                update.latestMetrics.getData(DataType.SPEED).lastOrNull()?.let { dp ->
                    trySend(
                        HealthSample.Location(
                            LocationSample(
                                speedMetersPerSec = dp.value.toFloat(),
                                accuracyMeters = ASSUMED_GPS_ACCURACY_METERS,
                                timestampMillis = now,
                            )
                        )
                    )
                }
                update.latestMetrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.let { dp ->
                    trySend(
                        HealthSample.Cadence(
                            CadenceSample(
                                stepsPerMinute = dp.value.toFloat(),
                                timestampMillis = now,
                            )
                        )
                    )
                }
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit
            override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
            override fun onRegistered() = Unit
            override fun onRegistrationFailed(throwable: Throwable) = Unit
        }

        exerciseClient.setUpdateCallback(callback)
        val config = ExerciseConfig.builder(ExerciseType.RUNNING)
            .setDataTypes(setOf(DataType.SPEED, DataType.STEPS_PER_MINUTE, DataType.LOCATION))
            .setIsGpsEnabled(true)
            .build()
        exerciseClient.startExerciseAsync(config)

        awaitClose {
            exerciseClient.endExerciseAsync()
            exerciseClient.clearUpdateCallbackAsync(callback)
        }
    }

    companion object {
        // Health Services exposes accuracy via the LOCATION data type, not SPEED. For MVP, we
        // assume samples emitted while an active ExerciseClient session is running are reliable.
        // A future iteration can extract per-sample horizontalPositionErrorMeters from LOCATION.
        private const val ASSUMED_GPS_ACCURACY_METERS: Float = 5f
    }
}
