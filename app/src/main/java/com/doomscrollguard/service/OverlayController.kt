package com.doomscrollguard.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.doomscrollguard.data.MonitoredApps
import com.doomscrollguard.engine.Intervention
import com.doomscrollguard.engine.InterventionLevel
import com.doomscrollguard.engine.SessionStats
import com.doomscrollguard.ui.overlay.IntentionScreen
import com.doomscrollguard.ui.overlay.InterventionCopy
import com.doomscrollguard.ui.overlay.NudgeCard
import com.doomscrollguard.ui.overlay.PauseScreen
import com.doomscrollguard.ui.overlay.formatDuration
import com.doomscrollguard.ui.overlay.formatMeters
import com.doomscrollguard.ui.overlay.pixelsToMeters
import com.doomscrollguard.ui.theme.DoomScrollGuardTheme

/** What the user did with an intervention screen. */
sealed interface OverlayResult {
    data object Left : OverlayResult
    data class Continued(val reason: String? = null) : OverlayResult
    /** Nudge closed (tapped OK or timed out); whether they left is decided later. */
    data object Dismissed : OverlayResult
}

/**
 * Draws interventions on top of other apps using an accessibility overlay
 * window, which needs no "draw over other apps" permission.
 */
class OverlayController(private val service: AccessibilityService) {

    private class Shown(
        val view: ComposeView,
        val owner: OverlayLifecycleOwner,
        val level: InterventionLevel,
    )

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismiss: Runnable? = null
    private var shown: Shown? = null

    val showingLevel: InterventionLevel? get() = shown?.level

    /**
     * Shows [intervention] unless an equal or stronger one is already on screen.
     * Returns true if it was shown. [onResult] fires once when the user acts.
     */
    fun show(
        intervention: Intervention,
        todayMs: Long?,
        onResult: (OverlayResult) -> Unit,
    ): Boolean {
        // Never stack overlays; a stronger level replaces a weaker one.
        shown?.let { current ->
            if (current.level >= intervention.level) return false
            dismiss()
        }

        val level = intervention.level
        val copy = copyFor(intervention.stats, todayMs)
        val finish: (OverlayResult) -> Unit = { result ->
            dismiss()
            onResult(result)
        }

        val owner = OverlayLifecycleOwner()
        val view = ComposeView(service)
        owner.attachTo(view)
        view.setContent {
            DoomScrollGuardTheme(darkTheme = true) {
                when (level) {
                    InterventionLevel.NUDGE -> NudgeCard(copy, onDismiss = { finish(OverlayResult.Dismissed) })
                    InterventionLevel.PAUSE -> PauseScreen(
                        copy = copy,
                        onLeave = { finish(OverlayResult.Left) },
                        onContinue = { finish(OverlayResult.Continued()) },
                    )
                    InterventionLevel.INTENTION -> IntentionScreen(
                        copy = copy,
                        onLeave = { finish(OverlayResult.Left) },
                        onContinue = { reason -> finish(OverlayResult.Continued(reason)) },
                    )
                }
            }
        }

        windowManager.addView(view, layoutParamsFor(level))
        shown = Shown(view, owner, level)

        if (level == InterventionLevel.NUDGE) {
            val r = Runnable { finish(OverlayResult.Dismissed) }
            autoDismiss = r
            handler.postDelayed(r, NUDGE_VISIBLE_MS)
        }
        return true
    }

    /** Removes whatever is on screen without reporting a result. */
    fun dismiss() {
        autoDismiss?.let(handler::removeCallbacks)
        autoDismiss = null
        val current = shown ?: return
        shown = null
        runCatching { windowManager.removeView(current.view) }
        current.owner.destroy()
    }

    private fun layoutParamsFor(level: InterventionLevel): WindowManager.LayoutParams {
        val fullScreen = level != InterventionLevel.NUDGE
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (level != InterventionLevel.INTENTION) {
            // Only the intention screen needs the keyboard.
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (!fullScreen) {
            // Let touches outside the card reach the app underneath.
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            if (fullScreen) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun copyFor(stats: SessionStats, todayMs: Long?): InterventionCopy {
        val ydpi = service.resources.displayMetrics.ydpi
        return InterventionCopy(
            appLabel = MonitoredApps.labelFor(stats.currentPackage, service.packageManager),
            duration = formatDuration(stats.activeMs),
            gestures = stats.gestures,
            distance = formatMeters(pixelsToMeters(stats.distancePx, ydpi)),
            todayTotal = todayMs?.takeIf { it > 0 }?.let(::formatDuration),
        )
    }

    private companion object {
        const val NUDGE_VISIBLE_MS = 8_000L
    }
}
