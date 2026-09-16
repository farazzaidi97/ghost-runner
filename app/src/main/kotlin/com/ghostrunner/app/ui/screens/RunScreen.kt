package com.ghostrunner.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ghostrunner.app.core.PacingEngineService
import com.ghostrunner.app.di.AppContainer
import com.ghostrunner.core.util.formatDelta
import com.ghostrunner.core.util.formatPaceMinPerKm
import com.ghostrunner.app.ui.theme.PaceAhead
import com.ghostrunner.app.ui.theme.PaceBehind
import com.ghostrunner.app.ui.theme.PaceOnPace
import com.ghostrunner.core.domain.PaceProfile
import com.ghostrunner.core.domain.PaceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    profileId: Long,
    container: AppContainer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf<PaceProfile?>(null) }
    val snapshot by container.livePacingSnapshot.collectAsState()

    LaunchedEffect(profileId) {
        profile = container.paceProfileRepository.get(profileId)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val critical = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (critical) {
            ContextCompat.startForegroundService(
                context,
                PacingEngineService.startIntent(context, profileId),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val activeProfile = profile
        if (activeProfile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(activeProfile.label, style = MaterialTheme.typography.headlineMedium)
            Text(
                "Target ${formatPaceMinPerKm(activeProfile.targetMetersPerSec)} · stride %.2f m"
                    .format(activeProfile.strideLengthMeters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.size(8.dp))

            val isRunning = snapshot != null
            StateBadge(snapshot?.state)

            if (snapshot != null) {
                val s = snapshot!!
                StatRow("Current", formatPaceMinPerKm(s.currentSpeedMps))
                StatRow("Δ", formatDelta(s.state.deltaMetersPerSec))
                StatRow("Speed", "%.2f m/s".format(s.currentSpeedMps))
                StatRow("Accuracy", if (s.accuracyMeters < 0f) "—" else "%.0f m".format(s.accuracyMeters))
                s.stepsPerMinute?.let { StatRow("Cadence", "%.0f spm".format(it)) }
                if (s.isStereoFallback) {
                    Text(
                        "Audio: stereo fallback (7.1 unavailable on this output)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "Tap Start. We'll request Location and Activity Recognition.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (isRunning) {
                        context.startService(PacingEngineService.stopIntent(context))
                    } else {
                        permissionLauncher.launch(requiredPermissions())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text(
                    if (isRunning) "Stop" else "Start",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun StateBadge(state: PaceState?) {
    val (label, color) = when (state) {
        null -> "Not running" to MaterialTheme.colorScheme.surfaceVariant
        is PaceState.Behind -> "BEHIND — ghost gaining" to PaceBehind
        is PaceState.OnPace -> "ON PACE" to PaceOnPace
        is PaceState.Ahead -> "AHEAD — gap widening" to PaceAhead
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (state == null) MaterialTheme.colorScheme.onSurface else color,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private fun requiredPermissions(): Array<String> {
    val base = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        base += Manifest.permission.POST_NOTIFICATIONS
    }
    return base.toTypedArray()
}
