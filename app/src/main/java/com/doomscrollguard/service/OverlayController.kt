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
import com.doomscrollguard.ui.overlay.InterventionCopy
import com.doomscrollguard.ui.overlay.NudgeCard
import com.doomscrollguard.ui.overlay.PauseScreen
import com.doomscrollguard.ui.overlay.formatDuration
import com.doomscrollguard.ui.overlay.formatMeters
import com.doomscrollguard.ui.overlay.pixelsToMeters
import com.doomscrollguard.ui.theme.DoomScrollGuardTheme

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
    private val autoDismiss = Runnable { dismiss() }
    private var shown: Shown? = null

    fun show(intervention: Intervention, onLeave: () -> Unit) {
        // Never stack overlays; a stronger level replaces a weaker one.
        shown?.let { current ->
            if (current.level >= intervention.level) return
            dismiss()
        }

        val level = intervention.level
        val copy = copyFor(intervention.stats)
        val owner = OverlayLifecycleOwner()
        val view = ComposeView(service)
        owner.attachTo(view)
        view.setContent {
            DoomScrollGuardTheme(darkTheme = true) {
                when (level) {
                    InterventionLevel.NUDGE -> NudgeCard(copy, onDismiss = ::dismiss)
                    InterventionLevel.PAUSE -> PauseScreen(
                        copy = copy,
                        onLeave = {
                            dismiss()
                            onLeave()
                        },
                        onContinue = ::dismiss,
                    )
                }
            }
        }

        windowManager.addView(view, layoutParamsFor(level))
        shown = Shown(view, owner, level)

        if (level == InterventionLevel.NUDGE) {
            handler.postDelayed(autoDismiss, NUDGE_VISIBLE_MS)
        }
    }

    fun dismiss() {
        handler.removeCallbacks(autoDismiss)
        val current = shown ?: return
        shown = null
        runCatching { windowManager.removeView(current.view) }
        current.owner.destroy()
    }

    private fun layoutParamsFor(level: InterventionLevel): WindowManager.LayoutParams {
        val fullScreen = level == InterventionLevel.PAUSE
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
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
        }
    }

    private fun copyFor(stats: SessionStats): InterventionCopy {
        val ydpi = service.resources.displayMetrics.ydpi
        return InterventionCopy(
            appLabel = MonitoredApps.labelFor(stats.currentPackage),
            duration = formatDuration(stats.activeMs),
            gestures = stats.gestures,
            distance = formatMeters(pixelsToMeters(stats.distancePx, ydpi)),
        )
    }

    private companion object {
        const val NUDGE_VISIBLE_MS = 8_000L
    }
}
