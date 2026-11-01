package com.doomscrollguard.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doomscrollguard.service.ServiceStatus
import com.doomscrollguard.ui.theme.Leaf

@Composable
fun StatusCard(enabled: Boolean, onEnable: () -> Unit) {
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
fun RestrictedSettingsHelp() {
    val context = LocalContext.current
    SectionCard(title = "Switch greyed out?") {
        Text(
            "Android blocks Accessibility for apps installed outside the Play Store until you allow it:\n" +
                "1. Tap \"Open app info\"\n" +
                "2. Tap ⋮ (top right) → \"Allow restricted settings\"\n" +
                "3. Come back and turn it on again",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { openAppInfo(context) }) { Text("Open app info") }
    }
}

/** Shown only on phones whose makers are known to kill background services. */
@Composable
fun BatteryHelpCard() {
    val context = LocalContext.current
    SectionCard(title = "Keep protection running") {
        Text(
            "Your phone may switch DoomScroll Guard off to save battery. To prevent this:\n" +
                "1. Tap \"Battery settings\"\n" +
                "2. Find DoomScroll Guard\n" +
                "3. Choose \"Unrestricted\" or \"Don't optimize\"",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { openBatterySettings(context) }) { Text("Battery settings") }
        TextButton(onClick = { openDontKillMyApp(context) }) { Text("Steps for my phone") }
    }
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun openBatterySettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        openAppInfo(context)
    }
}

/** Opens per-manufacturer instructions in the browser (this app itself has no internet access). */
private fun openDontKillMyApp(context: Context) {
    val url = "https://dontkillmyapp.com/${ServiceStatus.manufacturer}"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
