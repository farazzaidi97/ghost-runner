package com.ghostrunner.core.util

import kotlin.math.roundToInt

fun mpsToMinPerKm(mps: Float): Float {
    if (mps <= 0f) return Float.POSITIVE_INFINITY
    return 1000f / (mps * 60f)
}

fun formatPaceMinPerKm(mps: Float): String {
    if (mps <= 0f) return "—"
    val totalSeconds = (1000f / mps).roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d/km".format(minutes, seconds)
}

fun formatDelta(deltaMps: Float): String {
    val sign = if (deltaMps > 0) "+" else ""
    return "${sign}%.2f m/s".format(deltaMps)
}
