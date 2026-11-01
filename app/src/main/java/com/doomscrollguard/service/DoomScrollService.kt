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
import com.doomscrollguard.data.MonitoredApps
import com.doomscrollguard.data.SettingsRepository
import com.doomscrollguard.engine.Intervention
import com.doomscrollguard.engine.SessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watches which app is on screen and how much it's being scrolled. It only
 * looks at event types and package names: window content is never read
 * (canRetrieveWindowContent=false in the service config).
 */
class DoomScrollService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = SessionEngine()
    private lateinit var overlay: OverlayController

    private var monitored: Set<String> = MonitoredApps.defaultEnabled
    private var ignored: Set<String> = emptySet()
    private var tickJob: Job? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            leaveMonitoredApps()
        }
    }

    override fun onServiceConnected() {
        overlay = OverlayController(this)
        engine.fallbackGesturePx = (resources.displayMetrics.heightPixels * 0.6).toInt()
        ignored = buildIgnoredPackages()

        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        scope.launch {
            SettingsRepository(applicationContext).settings.collect { settings ->
                monitored = settings.monitoredPackages
                engine.updateThresholds(settings.thresholds)
                val fg = engine.foregroundPackage
                if (fg != null && fg !in monitored) leaveMonitoredApps()
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
                if (pkg !in monitored) return
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
        scope.cancel()
        runCatching { unregisterReceiver(screenOffReceiver) }
        if (::overlay.isInitialized) overlay.dismiss()
        super.onDestroy()
    }

    private fun onForegroundChanged(pkg: String, now: Long) {
        val wasActive = engine.isActive
        engine.onForeground(pkg, monitored = pkg in monitored, now = now)
        when {
            engine.isActive && !wasActive -> startTicking()
            !engine.isActive -> {
                stopTicking()
                overlay.dismiss()
            }
        }
    }

    private fun leaveMonitoredApps() {
        engine.onForeground(null, monitored = false, now = SystemClock.elapsedRealtime())
        stopTicking()
        if (::overlay.isInitialized) overlay.dismiss()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                handle(engine.tick(SystemClock.elapsedRealtime()))
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun handle(intervention: Intervention?) {
        intervention ?: return
        overlay.show(intervention, onLeave = { performGlobalAction(GLOBAL_ACTION_HOME) })
    }

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
    }
}
