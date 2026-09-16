package com.ghostrunner.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

internal val GhostMint = Color(0xFF7CF6C6)
internal val GhostMintDeep = Color(0xFF1FB686)
internal val GhostInk = Color(0xFF050B12)
internal val GhostMist = Color(0xFFB0BCC9)

internal val PaceBehind = Color(0xFFFF7E72)
internal val PaceOnPace = Color(0xFF7CF6C6)
internal val PaceAhead = Color(0xFF6FB8FF)

private val WearColors = Colors(
    primary = GhostMint,
    primaryVariant = GhostMintDeep,
    secondary = GhostMint,
    secondaryVariant = GhostMintDeep,
    background = GhostInk,
    surface = GhostInk,
    error = PaceBehind,
    onPrimary = GhostInk,
    onSecondary = GhostInk,
    onBackground = GhostMist,
    onSurface = GhostMist,
    onSurfaceVariant = GhostMist,
    onError = GhostInk,
)

@Composable
fun GhostRunnerWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearColors,
        content = content,
    )
}
