package com.doomscrollguard.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.doomscrollguard.service.DoomScrollService
import com.doomscrollguard.ui.home.AppsSection
import com.doomscrollguard.ui.home.BedtimeSection
import com.doomscrollguard.ui.home.GoalSection
import com.doomscrollguard.ui.home.PreviewSection
import com.doomscrollguard.ui.home.SectionCard
import com.doomscrollguard.ui.home.StatsSection
import com.doomscrollguard.ui.home.ToggleRow
import com.doomscrollguard.ui.overlay.IntentionScreen
import com.doomscrollguard.ui.overlay.InterventionCopy
import com.doomscrollguard.ui.overlay.NudgeCard
import com.doomscrollguard.ui.overlay.PauseScreen
import com.doomscrollguard.ui.theme.DoomScrollGuardTheme
import com.doomscrollguard.ui.theme.Leaf
import com.doomscrollguard.work.DailyRecapWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DoomScrollApp
        setContent {
            DoomScrollGuardTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(app)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(app: DoomScrollApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = app.settings
    val settings by repo.settings.collectAsState(initial = AppSettings())
    val summaryFlow = remember { app.stats.summary() }
    val summary by summaryFlow.collectAsState(initial = null as StatsSummary?)

    var serviceEnabled by remember { mutableStateOf(isServiceEnabled(context)) }
    var canNotify by remember { mutableStateOf(DailyRecapWorker.canNotify(context)) }
    LifecycleResumeEffect(Unit) {
        serviceEnabled = isServiceEnabled(context)
        canNotify = DailyRecapWorker.canNotify(context)
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> canNotify = granted }

    var showDisclosure by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<InterventionLevel?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("DoomScroll Guard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Gently interrupts endless scrolling so you can take your time back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        StatusCard(
            enabled = serviceEnabled,
            onEnable = {
                if (settings.disclosureAccepted) openAccessibilitySettings(context) else showDisclosure = true
            },
        )
        if (!serviceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RestrictedSettingsHelp(onOpenAppInfo = { openAppInfo(context) })
        }

        StatsSection(summary, settings.dailyGoalMinutes)

        GoalSection(settings.dailyGoalMinutes) { scope.launch { repo.setDailyGoal(it) } }

        AppsSection(settings.monitoredPackages) { pkgs, on ->
            scope.launch { pkgs.forEach { repo.setMonitored(it, on) } }
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

        PreviewSection { preview = it }

        Text(
            "Privacy: this app only sees which app is open and that you're scrolling. " +
                "It never reads what's on your screen and has no internet access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showDisclosure) {
        DisclosureDialog(
            onAccept = {
                showDisclosure = false
                scope.launch { repo.acceptDisclosure() }
                openAccessibilitySettings(context)
            },
            onDecline = { showDisclosure = false },
        )
    }

    preview?.let { level ->
        PreviewDialog(level = level, onDismiss = { preview = null })
    }
}

@Composable
private fun StatusCard(enabled: Boolean, onEnable: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Leaf.copy(alpha = 0.18f) else MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (enabled) "✅ Protection is ON" else "⚠️ Protection is OFF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (enabled) "Open a feed app and scroll. We'll step in when it's been too long."
                else "Turn on DoomScroll Guard in Accessibility settings so it can notice endless scrolling.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!enabled) {
                Button(onClick = onEnable) { Text("Turn on") }
            }
        }
    }
}

@Composable
private fun RestrictedSettingsHelp(onOpenAppInfo: () -> Unit) {
    SectionCard(title = "Toggle greyed out?") {
        Text(
            "Android blocks Accessibility for apps installed outside the Play Store until you allow it:\n" +
                "1. Tap \"Open app info\"\n" +
                "2. Tap ⋮ (top right) → \"Allow restricted settings\"\n" +
                "3. Come back and tap \"Turn on\" again",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onOpenAppInfo) { Text("Open app info") }
    }
}

@Composable
private fun DisclosureDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("How DoomScroll Guard works") },
        text = {
            Text(
                "DoomScroll Guard uses Android's Accessibility service to:\n\n" +
                    "• See which app is currently open\n" +
                    "• Notice when you scroll in the apps you choose\n" +
                    "• Show a reminder on top of those apps\n\n" +
                    "It does NOT read text, messages, or anything on your screen, " +
                    "and it has no internet access. Nothing leaves your phone.",
            )
        },
        confirmButton = { Button(onClick = onAccept) { Text("I agree") } },
        dismissButton = { TextButton(onClick = onDecline) { Text("Not now") } },
    )
}

@Composable
private fun PreviewDialog(level: InterventionLevel, onDismiss: () -> Unit) {
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
            when (level) {
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

private fun isServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, DoomScrollService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
