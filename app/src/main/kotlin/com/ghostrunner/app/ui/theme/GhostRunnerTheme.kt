package com.ghostrunner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = GhostMint,
    onPrimary = GhostInk,
    primaryContainer = GhostMintDeep,
    onPrimaryContainer = GhostInk,
    background = GhostInk,
    onBackground = GhostMist,
    surface = GhostInkSoft,
    onSurface = GhostMist,
    surfaceVariant = GhostInkSoft,
    onSurfaceVariant = GhostMist,
    error = PaceBehind,
)

private val LightColors = lightColorScheme(
    primary = GhostMintDeep,
    onPrimary = GhostInk,
    primaryContainer = GhostMint,
    onPrimaryContainer = GhostInk,
    background = androidx.compose.ui.graphics.Color.White,
    onBackground = GhostInk,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = GhostInk,
    error = PaceBehind,
)

@Composable
fun GhostRunnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = GhostTypography,
        content = content,
    )
}
