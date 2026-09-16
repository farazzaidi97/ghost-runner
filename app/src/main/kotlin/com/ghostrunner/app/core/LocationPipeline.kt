package com.ghostrunner.app.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.ghostrunner.core.domain.LocationSample
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationPipeline(context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    fun samples(): Flow<LocationSample> = callbackFlow {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MILLIS,
        ).setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(location.toSample())
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun Location.toSample(): LocationSample = LocationSample(
        speedMetersPerSec = if (hasSpeed()) speed else 0f,
        accuracyMeters = if (hasAccuracy()) accuracy else -1f,
        timestampMillis = time,
    )

    companion object {
        const val UPDATE_INTERVAL_MILLIS: Long = 2000L
        const val MIN_UPDATE_INTERVAL_MILLIS: Long = 1000L
    }
}
