package com.doomscrollguard.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.doomscrollguard.data.MonitoredApps
import com.doomscrollguard.data.Settings as AppSettings
import com.doomscrollguard.data.SettingsRepository
import com.doomscrollguard.engine.InterventionLevel
import com.doomscrollguard.service.DoomScrollService
import com.doomscrollguard.ui.overlay.InterventionCopy
import com.doomscrollguard.ui.overlay.NudgeCard
import com.doomscrollguard.ui.overlay.PauseScreen
import com.doomscrollguard.ui.theme.DoomScrollGuardTheme
import com.doomscrollguard.ui.theme.Leaf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = SettingsRepository(applicationContext)
        setContent {
            DoomScrollGuardTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(repo)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(repo: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsState(initial = AppSettings())

    var serviceEnabled by remember { mutableStateOf(isServiceEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        serviceEnabled = isServiceEnabled(context)
        onPauseOrDispose { }
    }

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

        SectionCard(title = "Apps to watch") {
            MonitoredApps.known.distinctBy { it.label }.forEach { app ->
                val pkgs = MonitoredApps.known.filter { it.label == app.label }.map { it.packageName }
                val checked = pkgs.any { it in settings.monitoredPackages }
                ToggleRow(label = app.label, checked = checked) { on ->
                    scope.launch { pkgs.forEach { repo.setMonitored(it, on) } }
                }
            }
        }

        SectionCard(title = "Demo mode") {
            ToggleRow(
                label = "Trigger fast (nudge after 30 sec or 8 swipes)",
                checked = settings.demoMode,
            ) { on -> scope.launch { repo.setDemoMode(on) } }
            Text(
                if (settings.demoMode) "Escalates to a full pause 20 sec after the nudge."
                else "Normal: nudge after 10 min or 150 swipes, then a full pause every 5 min.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Preview") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { preview = InterventionLevel.NUDGE }) { Text("Nudge") }
                OutlinedButton(onClick = { preview = InterventionLevel.PAUSE }) { Text("Pause screen") }
            }
        }

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
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
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
    val sample = InterventionCopy(appLabel = "Instagram", duration = "12 min", gestures = 184, distance = "14 m")
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
