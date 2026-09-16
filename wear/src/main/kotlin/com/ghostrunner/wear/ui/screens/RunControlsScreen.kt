package com.ghostrunner.wear.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.ghostrunner.core.domain.PaceProfile
import com.ghostrunner.core.domain.PaceState
import com.ghostrunner.core.util.formatDelta
import com.ghostrunner.core.util.formatPaceMinPerKm
import com.ghostrunner.wear.core.PacingEngineService
import com.ghostrunner.wear.di.AppContainer
import com.ghostrunner.wear.ui.theme.PaceAhead
import com.ghostrunner.wear.ui.theme.PaceBehind
import com.ghostrunner.wear.ui.theme.PaceOnPace

@Composable
fun RunControlsScreen(
    profileId: Long,
    container: AppContainer,
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf<PaceProfile?>(null) }
    val snapshot by container.livePacingSnapshot.collectAsState()

    LaunchedEffect(profileId) {
        profile = container.paceProfileRepository.get(profileId)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val critical = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
            grants[Manifest.permission.BODY_SENSORS] == true
        if (critical) {
            ContextCompat.startForegroundService(
                context,
                PacingEngineService.startIntent(context, profileId),
            )
        }
    }

    Scaffold(timeText = { TimeText() }) {
        val activeProfile = profile
        if (activeProfile == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Loading…", color = MaterialTheme.colors.onBackground)
            }
            return@Scaffold
        }
        val isRunning = snapshot != null
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = activeProfile.label,
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary,
            )
            Text(
                text = formatPaceMinPerKm(activeProfile.targetMetersPerSec),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )

            Spacer(Modifier.height(4.dp))

            val state = snapshot?.state
            StateBadge(state)

            snapshot?.let { s ->
                Text(
                    text = formatDelta(s.state.deltaMetersPerSec),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onBackground,
                )
                s.stepsPerMinute?.let { spm ->
                    Text(
                        text = "%.0f spm".format(spm),
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isRunning) {
                        context.startService(PacingEngineService.stopIntent(context))
                    } else {
                        launcher.launch(requiredPermissions())
                    }
                },
                colors = if (isRunning) {
                    ButtonDefaults.primaryButtonColors(
                        backgroundColor = MaterialTheme.colors.error,
                    )
                } else {
                    ButtonDefaults.primaryButtonColors()
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }
        }
    }
}

@Composable
private fun StateBadge(state: PaceState?) {
    val (label, color) = when (state) {
        null -> "Ready" to MaterialTheme.colors.onSurfaceVariant
        is PaceState.Behind -> "BEHIND" to PaceBehind
        is PaceState.OnPace -> "ON PACE" to PaceOnPace
        is PaceState.Ahead -> "AHEAD" to PaceAhead
    }
    Text(
        text = label,
        style = MaterialTheme.typography.title3,
        color = color,
    )
}

private fun requiredPermissions(): Array<String> {
    val base = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        base += Manifest.permission.POST_NOTIFICATIONS
    }
    return base.toTypedArray()
}
