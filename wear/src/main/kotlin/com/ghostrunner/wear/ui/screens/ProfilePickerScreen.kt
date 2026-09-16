package com.ghostrunner.wear.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.ghostrunner.core.data.PaceProfileRepository
import com.ghostrunner.core.util.formatPaceMinPerKm

@Composable
fun ProfilePickerScreen(
    repository: PaceProfileRepository,
    onSelect: (Long) -> Unit,
) {
    val profiles by repository.observeAll().collectAsState(initial = emptyList())
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp),
        ) {
            item {
                ListHeader { Text("Pick pace") }
            }
            items(profiles, key = { it.id }) { profile ->
                Chip(
                    onClick = { onSelect(profile.id) },
                    label = { Text(profile.label) },
                    secondaryLabel = { Text(formatPaceMinPerKm(profile.targetMetersPerSec)) },
                    colors = ChipDefaults.primaryChipColors(),
                )
            }
        }
    }
}
