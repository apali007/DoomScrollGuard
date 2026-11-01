package com.doomscrollguard.ui.overlay

import com.doomscrollguard.engine.MINUTE
import com.doomscrollguard.engine.SECOND
import java.util.Locale

fun formatDuration(ms: Long): String = when {
    ms >= MINUTE -> "${ms / MINUTE} min"
    else -> "${ms / SECOND} sec"
}

/** Converts scrolled pixels to physical metres using the screen's vertical DPI. */
fun pixelsToMeters(px: Long, ydpi: Float): Double =
    if (ydpi <= 0f) 0.0 else px / ydpi * 0.0254

fun formatMeters(meters: Double): String = when {
    meters >= 1000 -> String.format(Locale.US, "%.1f km", meters / 1000)
    meters >= 10 -> "${meters.toInt()} m"
    else -> String.format(Locale.US, "%.1f m", meters)
}
