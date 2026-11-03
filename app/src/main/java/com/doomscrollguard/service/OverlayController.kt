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
import com.doomscrollguard.engine.Strictness
import androidx.compose.runtime.Composable
import com.doomscrollguard.ui.overlay.BreakScreen
import com.doomscrollguard.ui.overlay.IntentionScreen
import com.doomscrollguard.ui.overlay.MindfulChoice
import com.doomscrollguard.ui.overlay.MindfulOpenScreen
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
        /** Null for screens that aren't interventions (opening question, break). */
        val level: InterventionLevel?,
        /** Higher replaces lower; equal or lower never replaces what's on screen. */
        val priority: Int,
    )

    /** Lock times and word counts for the pause/intention/break screens. */
    var strictness: Strictness = Strictness.BALANCED

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismiss: Runnable? = null
    private var shown: Shown? = null

    val showingLevel: InterventionLevel? get() = shown?.level

    val isShowingBreak: Boolean get() = shown?.priority == PRIORITY_BREAK

    /**
     * Shows [intervention] unless an equal or stronger one is already on screen.
     * Returns true if it was shown. [onResult] fires once when the user acts.
     */
    fun show(
        intervention: Intervention,
        todayMs: Long?,
        onResult: (OverlayResult) -> Unit,
    ): Boolean {
        val level = intervention.level
        if (!makeRoomFor(priorityOf(level))) return false

        val copy = copyFor(intervention.stats, todayMs)
        val finish: (OverlayResult) -> Unit = { result ->
            dismiss()
            onResult(result)
        }

        val rules = strictness
        addOverlay(level, priorityOf(level), layoutParamsFor(level)) {
            when (level) {
                InterventionLevel.NUDGE -> NudgeCard(copy, onDismiss = { finish(OverlayResult.Dismissed) })
                InterventionLevel.PAUSE -> PauseScreen(
                    copy = copy,
                    lockSeconds = rules.pauseLockSeconds,
                    onLeave = { finish(OverlayResult.Left) },
                    onContinue = { finish(OverlayResult.Continued()) },
                )
                InterventionLevel.INTENTION -> IntentionScreen(
                    copy = copy,
                    lockSeconds = rules.intentionLockSeconds,
                    minWords = rules.intentionMinWords,
                    onLeave = { finish(OverlayResult.Left) },
                    onContinue = { reason -> finish(OverlayResult.Continued(reason)) },
                )
            }
        }

        if (level == InterventionLevel.NUDGE) {
            val r = Runnable { finish(OverlayResult.Dismissed) }
            autoDismiss = r
            handler.postDelayed(r, NUDGE_VISIBLE_MS)
        }
        return true
    }

    /** Asks why the user is opening [pkg]. Skipped if anything else is on screen. */
    fun showMindfulOpen(pkg: String, onChoice: (MindfulChoice) -> Unit): Boolean {
        if (shown != null) return false
        val label = MonitoredApps.labelFor(pkg, service.packageManager)
        val params = layoutParamsFor(InterventionLevel.PAUSE) // full screen, not focusable
        addOverlay(level = null, priority = PRIORITY_MINDFUL, params = params) {
            MindfulOpenScreen(label) { choice ->
                dismiss()
                onChoice(choice)
            }
        }
        return true
    }

    /**
     * Covers [pkg] with the break countdown. Replaces anything else on screen.
     * Exactly one of the callbacks fires, after the overlay is removed.
     */
    fun showBreak(
        pkg: String,
        endsAtElapsed: Long,
        onGoHome: () -> Unit,
        onOpenAnyway: () -> Unit,
        onFinished: () -> Unit,
    ): Boolean {
        if (!makeRoomFor(PRIORITY_BREAK)) return false
        val label = MonitoredApps.labelFor(pkg, service.packageManager)
        val skipAfter = strictness.breakSkipAfterSeconds
        val then: (() -> Unit) -> Unit = { callback ->
            dismiss()
            callback()
        }
        addOverlay(level = null, priority = PRIORITY_BREAK, params = layoutParamsFor(InterventionLevel.PAUSE)) {
            BreakScreen(
                appLabel = label,
                endsAtElapsed = endsAtElapsed,
                skipAfterSeconds = skipAfter,
                onGoHome = { then(onGoHome) },
                onOpenAnyway = { then(onOpenAnyway) },
                onFinished = { then(onFinished) },
            )
        }
        return true
    }

    /** Clears a weaker overlay so one of [priority] can show. False if something as strong is up. */
    private fun makeRoomFor(priority: Int): Boolean {
        val current = shown ?: return true
        if (current.priority >= priority) return false
        dismiss()
        return true
    }

    private fun priorityOf(level: InterventionLevel): Int = PRIORITY_NUDGE + level.ordinal

    private fun addOverlay(
        level: InterventionLevel?,
        priority: Int,
        params: WindowManager.LayoutParams,
        content: @Composable () -> Unit,
    ) {
        val owner = OverlayLifecycleOwner()
        val view = ComposeView(service)
        owner.attachTo(view)
        view.setContent { DoomScrollGuardTheme(darkTheme = true) { content() } }
        windowManager.addView(view, params)
        shown = Shown(view, owner, level, priority)
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
            // The nudge card floats in the middle of the screen; full-screen overlays fill it.
            gravity = if (fullScreen) Gravity.TOP else Gravity.CENTER
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
        const val PRIORITY_MINDFUL = 0
        const val PRIORITY_NUDGE = 1 // PAUSE = 2, INTENTION = 3
        const val PRIORITY_BREAK = 10
    }
}
