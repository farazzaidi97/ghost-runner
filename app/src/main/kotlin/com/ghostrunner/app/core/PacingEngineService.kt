package com.ghostrunner.app.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ghostrunner.app.GhostRunnerApplication
import com.ghostrunner.app.R
import com.ghostrunner.core.audio.AudioFocusController
import com.ghostrunner.core.audio.SpatialAudioRenderer
import com.ghostrunner.app.ui.MainActivity
import com.ghostrunner.core.domain.PaceProfile
import com.ghostrunner.core.domain.PaceState
import com.ghostrunner.core.engine.MovingAverageFilter
import com.ghostrunner.core.engine.PacingEngine
import com.ghostrunner.core.runtime.PacingSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class PacingEngineService : LifecycleService() {

    private val audioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var renderer: SpatialAudioRenderer
    private lateinit var focusController: AudioFocusController
    private lateinit var engine: PacingEngine

    private var location: LocationPipeline? = null
    private var stepDetector: StepDetectorSource? = null

    private var activeProfile: PaceProfile? = null
    private var footstepJob: Job? = null
    private var lastSpeedMps: Float = 0f
    private var lastAccuracy: Float = -1f
    private var lastStepsPerMinute: Float? = null

    override fun onCreate() {
        super.onCreate()
        renderer = SpatialAudioRenderer()
        focusController = AudioFocusController(this)
        engine = PacingEngine(filter = MovingAverageFilter())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP_PACING -> {
                stopPacing()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_PACING -> {
                val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
                if (profileId <= 0L) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                promoteToForeground()
                startPacing(profileId)
                return START_STICKY
            }
            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    private fun promoteToForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
        )
    }

    private fun startPacing(profileId: Long) {
        val container = (application as GhostRunnerApplication).container
        lifecycleScope.launch {
            val profile = container.paceProfileRepository.get(profileId) ?: run {
                stopSelf()
                return@launch
            }
            activeProfile = profile
            engine.reset()

            location = LocationPipeline(this@PacingEngineService)
            stepDetector = StepDetectorSource(this@PacingEngineService)
            renderer.start()
            footstepJob = launchFootstepLoop()

            stepDetector?.samples()
                ?.onEach { cadence ->
                    engine.onCadence(cadence)
                    lastStepsPerMinute = cadence.stepsPerMinute
                    publishSnapshot(engine.state)
                }
                ?.launchIn(lifecycleScope)

            location?.samples()
                ?.onEach { sample ->
                    lastSpeedMps = sample.speedMetersPerSec
                    lastAccuracy = sample.accuracyMeters
                    val state = engine.onLocation(sample, profile)
                    focusController.applyForState(state)
                    publishSnapshot(state)
                }
                ?.launchIn(lifecycleScope)
        }
    }

    private fun launchFootstepLoop(): Job = audioScope.launch {
        val intervalMs = 60_000L / FOOTSTEP_BPM
        while (isActive) {
            renderer.enqueueFootstep(engine.state)
            delay(intervalMs)
        }
    }

    private fun publishSnapshot(state: PaceState) {
        val container = (application as GhostRunnerApplication).container
        val profile = activeProfile ?: return
        container.emitSnapshot(
            PacingSnapshot(
                profile = profile,
                state = state,
                currentSpeedMps = lastSpeedMps,
                accuracyMeters = lastAccuracy,
                stepsPerMinute = lastStepsPerMinute,
                isStereoFallback = renderer.isStereoFallback,
            )
        )
    }

    private fun stopPacing() {
        footstepJob?.cancel()
        footstepJob = null
        focusController.release()
        renderer.stop()
        engine.reset()
        location = null
        stepDetector = null
        activeProfile = null
        (application as GhostRunnerApplication).container.emitSnapshot(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopPacing()
        audioScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PacingEngineService::class.java).setAction(ACTION_STOP_PACING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, PACING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(tapIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(PACING_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            PACING_CHANNEL_ID,
            getString(R.string.pacing_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.pacing_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START_PACING = "com.ghostrunner.app.ACTION_START_PACING"
        const val ACTION_STOP_PACING = "com.ghostrunner.app.ACTION_STOP_PACING"
        const val EXTRA_PROFILE_ID = "com.ghostrunner.app.EXTRA_PROFILE_ID"

        private const val PACING_CHANNEL_ID = "PACING_CHANNEL"
        private const val NOTIFICATION_ID = 1
        private const val FOOTSTEP_BPM = 180

        fun startIntent(context: Context, profileId: Long): Intent =
            Intent(context, PacingEngineService::class.java).apply {
                action = ACTION_START_PACING
                putExtra(EXTRA_PROFILE_ID, profileId)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, PacingEngineService::class.java).apply {
                action = ACTION_STOP_PACING
            }
    }
}
