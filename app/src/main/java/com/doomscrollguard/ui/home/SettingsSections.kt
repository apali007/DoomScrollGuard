package com.doomscrollguard.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doomscrollguard.data.InstalledApp
import com.doomscrollguard.data.MonitoredApps
import com.doomscrollguard.data.Settings
import com.doomscrollguard.engine.InterventionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GoalSection(goalMinutes: Int, onGoalChange: (Int) -> Unit) {
    // Local slider state so dragging is smooth; persisted when released.
    var value by remember(goalMinutes) { mutableFloatStateOf(goalMinutes.toFloat()) }
    SectionCard(title = "Daily goal") {
        Text("At most ${value.toInt()} min a day in feeds", fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onGoalChange(value.toInt()) },
            valueRange = 15f..120f,
            steps = 6, // 15-minute increments
        )
    }
}

@Composable
fun AppsSection(monitored: Set<String>, onToggle: (packages: List<String>, enabled: Boolean) -> Unit) {
    val context = LocalContext.current
    val installed by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            MonitoredApps.installedLaunchable(context.packageManager, context.packageName)
        }
    }
    var showOthers by rememberSaveable { mutableStateOf(false) }

    SectionCard(title = "Apps to watch") {
        val apps = installed
        if (apps == null) {
            Text("Loading apps…")
            return@SectionCard
        }
        val installedPkgs = apps.map { it.packageName }.toSet()
        val suggested = MonitoredApps.known
            .filter { it.packageName in installedPkgs }
            .groupBy { it.label }

        if (suggested.isEmpty()) {
            Text(
                "None of the usual feed apps are installed. Pick yours below.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        suggested.forEach { (label, known) ->
            val pkgs = known.map { it.packageName }
            ToggleRow(label = label, checked = pkgs.any { it in monitored }) { on -> onToggle(pkgs, on) }
        }

        val knownPkgs = MonitoredApps.known.map { it.packageName }.toSet()
        val others = apps.filter { it.packageName !in knownPkgs }
        val othersOn = others.count { it.packageName in monitored }
        TextButton(onClick = { showOthers = !showOthers }) {
            Text(
                (if (showOthers) "Hide other apps" else "Add other apps") +
                    if (othersOn > 0) " ($othersOn on)" else "",
            )
        }
        if (showOthers) {
            others.forEach { app ->
                ToggleRow(label = app.label, checked = app.packageName in monitored) { on ->
                    onToggle(listOf(app.packageName), on)
                }
            }
        }
    }
}

@Composable
fun BedtimeSection(
    settings: Settings,
    onEnabledChange: (Boolean) -> Unit,
    onHoursChange: (start: Int, end: Int) -> Unit,
) {
    SectionCard(title = "Bedtime mode") {
        ToggleRow(
            label = "Stricter at night",
            subtitle = "Reminders come twice as fast during these hours.",
            checked = settings.bedtimeEnabled,
            onChange = onEnabledChange,
        )
        if (settings.bedtimeEnabled) {
            HourStepper("Starts", settings.bedtimeStartHour) { onHoursChange(it, settings.bedtimeEndHour) }
            HourStepper("Ends", settings.bedtimeEndHour) { onHoursChange(settings.bedtimeStartHour, it) }
        }
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = { onChange((hour + 23) % 24) }) { Text("−") }
        Text(formatHour(hour), fontWeight = FontWeight.Medium)
        TextButton(onClick = { onChange((hour + 1) % 24) }) { Text("+") }
    }
}

fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}

@Composable
fun PreviewSection(onPreview: (InterventionLevel) -> Unit) {
    SectionCard(title = "Preview") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onPreview(InterventionLevel.NUDGE) }) { Text("Nudge") }
            OutlinedButton(onClick = { onPreview(InterventionLevel.PAUSE) }) { Text("Pause") }
            OutlinedButton(onClick = { onPreview(InterventionLevel.INTENTION) }) { Text("Intention") }
        }
    }
}
