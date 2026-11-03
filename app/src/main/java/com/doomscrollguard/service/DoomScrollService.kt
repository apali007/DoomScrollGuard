package com.doomscrollguard.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.doomscrollguard.DoomScrollApp
import com.doomscrollguard.data.Settings
import com.doomscrollguard.data.StatsRepository
import com.doomscrollguard.data.db.Outcome
import com.doomscrollguard.engine.BreakTimer
import com.doomscrollguard.engine.Intervention
import com.doomscrollguard.engine.InterventionLevel
import com.doomscrollguard.engine.MINUTE
import com.doomscrollguard.engine.SessionEngine
import com.doomscrollguard.ui.overlay.MindfulChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Watches which app is on screen and how much it's being scrolled. It only
 * looks at event types and package names: window content is never read
 * (canRetrieveWindowContent=false in the service config).
 */
class DoomScrollService : AccessibilityService() {

    /** An intervention whose outcome we haven't recorded yet. */
    private class OpenIntervention(
        val id: Deferred<Long>,
        val level: InterventionLevel,
        val shownAt: Long,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = SessionEngine()
    /** After leaving from a pause/intention, watched apps show a break screen until this ends. */
    private val breakTimer = BreakTimer()
    private lateinit var overlay: OverlayController
    private lateinit var stats: StatsRepository

    private var settings = Settings()
    private var ignored: Set<String> = emptySet()
    private var tickJob: Job? = null
    private var open: OpenIntervention? = null

    // Feed time not yet written to the database.
    private var usagePkg: String? = null
    private var usageSince: Long = 0L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onScreenOff()
        }
    }

    override fun onServiceConnected() {
        overlay = OverlayController(this)
        val app = application as DoomScrollApp
        stats = app.stats
        engine.fallbackGesturePx = (resources.displayMetrics.heightPixels * 0.6).toInt()
        ignored = buildIgnoredPackages()

        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        scope.launch { app.settings.markServiceRan() }
        scope.launch {
            app.settings.settings.collect { newSettings ->
                settings = newSettings
                overlay.strictness = newSettings.strictness
                engine.updateThresholds(newSettings.thresholds)
                val fg = engine.foregroundPackage
                if (fg != null && fg !in settings.monitoredPackages) leaveMonitoredApps()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored) return
        val now = SystemClock.elapsedRealtime()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onForegroundChanged(pkg, now)

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (pkg !in settings.monitoredPackages) return
                // A scroll proves the app is on screen, e.g. after unlocking the
                // phone straight back into a feed without a window change event.
                if (engine.foregroundPackage != pkg) onForegroundChanged(pkg, now)
                val delta = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaY else 0
                handle(engine.onScroll(delta, now))
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        leaveMonitoredApps()
        scope.cancel()
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    private fun onForegroundChanged(pkg: String, now: Long) {
        val monitored = pkg in settings.monitoredPackages
        if (monitored && breakTimer.isActive(now)) {
            coverWithBreak(pkg)
            return
        }
        // Moved on to a non-watched app: the break screen must not follow them there.
        if (!monitored && overlay.isShowingBreak) overlay.dismiss()

        val wasActive = engine.isActive
        val previous = engine.foregroundPackage
        val sessionsBefore = engine.sessionCount
        engine.onForeground(pkg, monitored = monitored, now = now)
        if (engine.foregroundPackage != previous) recordUsage(now, engine.foregroundPackage)

        when {
            engine.isActive && !wasActive -> startTicking()
            !engine.isActive && wasActive -> onLeftFeeds(now)
        }
        if (engine.sessionCount != sessionsBefore) {
            askWhyOpening(pkg)
            applyDailyGoal()
        }
    }

    /** Past today's goal: every new visit gets its first nudge after a minute. */
    private fun applyDailyGoal() {
        scope.launch {
            val todayMs = runCatching { stats.summaryOnce().todayMs }.getOrNull() ?: return@launch
            if (todayMs + unflushedUsageMs(SystemClock.elapsedRealtime()) >= dailyGoalMs()) {
                engine.nudgeCurrentSessionAfter(OVER_GOAL_NUDGE_MS)
            }
        }
    }

    private fun dailyGoalMs(): Long = settings.dailyGoalMinutes * MINUTE

    /** On every fresh visit to a watched app (not a quick return), ask what they came for. */
    private fun askWhyOpening(pkg: String) {
        overlay.showMindfulOpen(pkg) { choice ->
            when (choice) {
                MindfulChoice.PURPOSE -> Unit
                MindfulChoice.BROWSING -> engine.scaleCurrentSessionNudge(JUST_BROWSING_NUDGE_SCALE)
                MindfulChoice.NEVER_MIND -> performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    /**
     * A watched app was opened during a break: show the countdown instead of the
     * feed. Time on the break screen doesn't count as feed time.
     */
    private fun coverWithBreak(pkg: String) {
        leaveMonitoredApps()
        overlay.showBreak(
            pkg = pkg,
            endsAtElapsed = breakTimer.endsAtMs,
            onGoHome = { performGlobalAction(GLOBAL_ACTION_HOME) },
            onOpenAnyway = {
                breakTimer.cancel()
                onForegroundChanged(pkg, SystemClock.elapsedRealtime())
            },
            // Break ran out while they waited on the screen: let them in.
            onFinished = { onForegroundChanged(pkg, SystemClock.elapsedRealtime()) },
        )
    }

    private fun startBreak() {
        breakTimer.start(SystemClock.elapsedRealtime(), settings.strictness.breakMs)
        // Coming back after the break continues the escalation rather than starting over.
        engine.holdSessionUntil(breakTimer.endsAtMs)
    }

    private fun leaveMonitoredApps() {
        if (!::overlay.isInitialized) return
        val now = SystemClock.elapsedRealtime()
        val wasActive = engine.isActive
        engine.onForeground(null, monitored = false, now = now)
        recordUsage(now, null)
        if (wasActive) onLeftFeeds(now)
    }

    private fun onScreenOff() {
        leaveMonitoredApps()
        if (::overlay.isInitialized) overlay.dismiss()
    }

    /** The user is no longer in any monitored app. */
    private fun onLeftFeeds(now: Long) {
        stopTicking()
        overlay.dismiss()
        open?.let { o ->
            // Leaving soon after a nudge counts as the nudge working.
            val counted = o.level != InterventionLevel.NUDGE || now - o.shownAt <= NUDGE_LEFT_WINDOW_MS
            closeOpen(if (counted) Outcome.LEFT else Outcome.IGNORED)
            // Pressing Home/Recents on a pause or intention screen is leaving too,
            // so it earns a break; otherwise it'd be a way around the "Leave" button.
            if (o.level != InterventionLevel.NUDGE) startBreak()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            var ticks = 0
            while (isActive) {
                delay(TICK_MS)
                val now = SystemClock.elapsedRealtime()
                handle(engine.tick(now))
                expireStaleNudge(now)
                if (++ticks % FLUSH_EVERY_TICKS == 0) recordUsage(now, engine.foregroundPackage)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun handle(intervention: Intervention?) {
        intervention ?: return
        scope.launch {
            val todayMs = runCatching { stats.summaryOnce().todayMs }.getOrNull()
                ?.plus(unflushedUsageMs(SystemClock.elapsedRealtime()))
            if (!engine.isActive) return@launch // left while we were loading stats

            val shown = overlay.show(intervention, todayMs, dailyGoalMs(), ::onOverlayResult)
            if (!shown) return@launch
            closeOpen(Outcome.IGNORED) // a new intervention supersedes the previous one
            val pkg = intervention.stats.currentPackage
            open = OpenIntervention(
                id = scope.async { stats.logIntervention(pkg, intervention.level) },
                level = intervention.level,
                shownAt = SystemClock.elapsedRealtime(),
            )
        }
    }

    private fun onOverlayResult(result: OverlayResult) {
        when (result) {
            OverlayResult.Left -> {
                closeOpen(Outcome.LEFT)
                startBreak()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            is OverlayResult.Continued -> closeOpen(Outcome.CONTINUED, result.reason)
            // A dismissed nudge stays open: leaving within a minute still counts.
            OverlayResult.Dismissed -> Unit
        }
    }

    private fun expireStaleNudge(now: Long) {
        val o = open ?: return
        if (o.level == InterventionLevel.NUDGE &&
            overlay.showingLevel == null &&
            now - o.shownAt > NUDGE_LEFT_WINDOW_MS
        ) {
            closeOpen(Outcome.IGNORED)
        }
    }

    private fun closeOpen(outcome: Outcome, reason: String? = null) {
        val o = open ?: return
        open = null
        scope.launch {
            withContext(NonCancellable) {
                runCatching { stats.setOutcome(o.id.await(), outcome, reason) }
            }
        }
    }

    /** Writes feed time accumulated for the current app, then starts tracking [nextPkg]. */
    private fun recordUsage(now: Long, nextPkg: String?) {
        val pkg = usagePkg
        val ms = unflushedUsageMs(now)
        if (pkg != null && ms > 0) {
            scope.launch {
                withContext(NonCancellable) { runCatching { stats.addUsage(pkg, ms) } }
            }
        }
        usagePkg = nextPkg
        usageSince = now
    }

    private fun unflushedUsageMs(now: Long): Long = if (usagePkg != null) now - usageSince else 0L

    /** Windows that appear over a feed without meaning the user left it. */
    private fun buildIgnoredPackages(): Set<String> {
        val keyboards = getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            .orEmpty()
        return setOf(packageName, "com.android.systemui") + keyboards
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val FLUSH_EVERY_TICKS = 60
        /** Leaving within this long after a nudge counts as "left". */
        const val NUDGE_LEFT_WINDOW_MS = 60_000L
        /** "Just browsing" on the opening question makes the first nudge come twice as fast. */
        const val JUST_BROWSING_NUDGE_SCALE = 0.5
        /** Once past the daily goal, how soon each new visit gets nudged. */
        const val OVER_GOAL_NUDGE_MS = MINUTE
    }
}
