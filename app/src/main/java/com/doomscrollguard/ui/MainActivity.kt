package com.doomscrollguard.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.doomscrollguard.DoomScrollApp
import com.doomscrollguard.data.Settings as AppSettings
import com.doomscrollguard.data.StatsSummary
import com.doomscrollguard.engine.InterventionLevel
import com.doomscrollguard.service.ServiceStatus
import com.doomscrollguard.ui.home.AppsSection
import com.doomscrollguard.ui.home.BatteryHelpCard
import com.doomscrollguard.ui.home.BedtimeSection
import com.doomscrollguard.ui.home.GoalSection
import com.doomscrollguard.ui.home.PreviewSection
import com.doomscrollguard.ui.home.RestrictedSettingsHelp
import com.doomscrollguard.ui.home.SectionCard
import com.doomscrollguard.ui.home.StatsSection
import com.doomscrollguard.ui.home.StatusCard
import com.doomscrollguard.ui.home.ToggleRow
import com.doomscrollguard.ui.home.openAccessibilitySettings
import com.doomscrollguard.ui.onboarding.OnboardingScreen
import com.doomscrollguard.ui.overlay.IntentionScreen
import com.doomscrollguard.ui.overlay.InterventionCopy
import com.doomscrollguard.ui.overlay.MindfulOpenScreen
import com.doomscrollguard.ui.overlay.NudgeCard
import com.doomscrollguard.ui.overlay.PauseScreen
import com.doomscrollguard.ui.theme.DoomScrollGuardTheme
import com.doomscrollguard.work.DailyRecapWorker
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DoomScrollApp
        setContent {
            DoomScrollGuardTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val settings by app.settings.settings.collectAsState(initial = null as AppSettings?)
                    val loaded = settings ?: return@Surface // avoid flashing onboarding before prefs load
                    if (loaded.onboardingDone) {
                        HomeScreen(app, loaded)
                    } else {
                        OnboardingScreen(loaded, app.settings)
                    }
                }
            }
        }
    }
}

/** A preview of an overlay screen, shown inside the app. */
private sealed interface Preview {
    data class Level(val level: InterventionLevel) : Preview
    data object MindfulOpen : Preview
}

@Composable
private fun HomeScreen(app: DoomScrollApp, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = app.settings
    val summaryFlow = remember { app.stats.summary() }
    val summary by summaryFlow.collectAsState(initial = null as StatsSummary?)

    var serviceEnabled by remember { mutableStateOf(ServiceStatus.isEnabled(context)) }
    var canNotify by remember { mutableStateOf(DailyRecapWorker.canNotify(context)) }
    var needsBatteryHelp by remember { mutableStateOf(ServiceStatus.needsBatteryHelp(context)) }
    LifecycleResumeEffect(Unit) {
        serviceEnabled = ServiceStatus.isEnabled(context)
        canNotify = DailyRecapWorker.canNotify(context)
        needsBatteryHelp = ServiceStatus.needsBatteryHelp(context)
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> canNotify = granted }

    var preview by remember { mutableStateOf<Preview?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("DoomScroll Guard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        StatusCard(enabled = serviceEnabled, onEnable = { openAccessibilitySettings(context) })
        if (!serviceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RestrictedSettingsHelp()
        }
        if (serviceEnabled && needsBatteryHelp) {
            BatteryHelpCard()
        }

        StatsSection(summary, settings.dailyGoalMinutes)

        GoalSection(settings.dailyGoalMinutes) { scope.launch { repo.setDailyGoal(it) } }

        AppsSection(settings.monitoredPackages) { pkgs, on ->
            scope.launch { pkgs.forEach { repo.setMonitored(it, on) } }
        }

        SectionCard(title = "Smarter reminders") {
            ToggleRow(
                label = "Adapt to my habits",
                subtitle = adaptiveSubtitle(settings.adaptiveEnabled, summary),
                checked = settings.adaptiveEnabled,
            ) { on -> scope.launch { repo.setAdaptiveEnabled(on) } }
            ToggleRow(
                label = "Ask why I'm opening a feed",
                subtitle = "A quick question when you open a watched app. \"Just browsing\" means earlier reminders.",
                checked = settings.mindfulOpenEnabled,
            ) { on -> scope.launch { repo.setMindfulOpenEnabled(on) } }
        }

        BedtimeSection(
            settings = settings,
            onEnabledChange = { scope.launch { repo.setBedtimeEnabled(it) } },
            onHoursChange = { start, end -> scope.launch { repo.setBedtimeHours(start, end) } },
        )

        SectionCard(title = "Daily recap") {
            ToggleRow(
                label = "Evening summary at 9 PM",
                checked = settings.recapEnabled,
            ) { on ->
                scope.launch { repo.setRecapEnabled(on) }
                if (on && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (settings.recapEnabled && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                TextButton(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Allow notifications")
                }
            }
        }

        SectionCard(title = "Demo mode") {
            ToggleRow(
                label = "Trigger fast",
                subtitle = if (settings.demoMode) "Nudge after 30 sec or 8 swipes, then escalate every 20 sec."
                else "Normal: nudge after 10 min or 150 swipes, then escalate every 5 min.",
                checked = settings.demoMode,
            ) { on -> scope.launch { repo.setDemoMode(on) } }
        }

        PreviewSection(
            onPreview = { preview = Preview.Level(it) },
            onPreviewMindful = { preview = Preview.MindfulOpen },
        )

        Text(
            "Privacy: this app only sees which app is open and that you're scrolling. " +
                "It never reads what's on your screen and has no internet access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    preview?.let { PreviewDialog(it, onDismiss = { preview = null }) }
}

private fun adaptiveSubtitle(enabled: Boolean, summary: StatsSummary?): String {
    if (!enabled) return "Off: reminders always use the normal timing."
    val factor = summary?.adaptiveFactor ?: 1.0
    if (factor >= 1.0) return "Reminders come sooner if you often keep scrolling after a pause. Right now: normal timing."
    val sooner = ((1 - factor) * 100).roundToInt()
    return "Right now: reminders come $sooner% sooner, because you kept scrolling after " +
        "${summary?.recentContinued} of ${summary?.recentDecided} pauses this week."
}

@Composable
private fun PreviewDialog(preview: Preview, onDismiss: () -> Unit) {
    val sample = InterventionCopy(
        appLabel = "Instagram",
        duration = "12 min",
        gestures = 184,
        distance = "14 m",
        todayTotal = "1 h 5 min",
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        DoomScrollGuardTheme(darkTheme = true) {
            when (preview) {
                Preview.MindfulOpen -> MindfulOpenScreen("Instagram") { onDismiss() }
                is Preview.Level -> when (preview.level) {
                    InterventionLevel.NUDGE -> NudgeCard(sample, onDismiss = onDismiss)
                    InterventionLevel.PAUSE -> PauseScreen(
                        copy = sample,
                        lockSeconds = 3,
                        onLeave = onDismiss,
                        onContinue = onDismiss,
                    )
                    InterventionLevel.INTENTION -> IntentionScreen(
                        copy = sample,
                        lockSeconds = 3,
                        onLeave = onDismiss,
                        onContinue = { onDismiss() },
                    )
                }
            }
        }
    }
}
