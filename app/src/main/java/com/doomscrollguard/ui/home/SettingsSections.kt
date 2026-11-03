package com.doomscrollguard.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.doomscrollguard.data.InstalledApp
import com.doomscrollguard.data.MonitoredApps
import com.doomscrollguard.data.Settings
import com.doomscrollguard.engine.SECOND
import com.doomscrollguard.engine.Strictness
import com.doomscrollguard.ui.overlay.formatDuration
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
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    SectionCard(title = "Apps to watch") {
        val apps = installed
        if (apps == null) {
            Text("Loading apps…")
            return@SectionCard
        }

        // Only what the user turned on, so the list stays short and personal.
        val watching = apps.filter { it.packageName in monitored }
        if (watching.isEmpty()) {
            Text(
                "No apps yet. Add the apps where you lose track of time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        watching.forEach { app ->
            ToggleRow(label = app.label, checked = true) { on -> onToggle(listOf(app.packageName), on) }
        }

        TextButton(onClick = { showPicker = !showPicker }) {
            Text(if (showPicker) "Done" else "+ Add apps")
        }
        if (showPicker) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps") },
                singleLine = true,
            )
            val knownPkgs = MonitoredApps.known.map { it.packageName }.toSet()
            apps
                .filter { it.packageName !in monitored }
                .filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
                // Common feed apps first (if installed), then everything else alphabetically.
                .sortedBy { if (it.packageName in knownPkgs) 0 else 1 }
                .forEach { app ->
                    ToggleRow(label = app.label, checked = false) { on ->
                        onToggle(listOf(app.packageName), on)
                    }
                }
        }
    }
}

@Composable
fun StrictnessSection(selected: Strictness, onSelect: (Strictness) -> Unit) {
    SectionCard(title = "Strictness") {
        Strictness.entries.forEach { level ->
            val isSelected = level == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent,
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(level) })
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Column(Modifier.padding(start = 8.dp)) {
                    Text("${level.emoji} ${level.label}", fontWeight = FontWeight.SemiBold)
                    Text(
                        strictnessSummary(level),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

fun strictnessSummary(level: Strictness): String {
    val t = level.thresholds
    val skip = level.breakSkipAfterSeconds?.let { "skippable after ${formatDuration(it * SECOND)}" } ?: "can't be skipped"
    return "Nudge after ${formatDuration(t.nudgeAfterMs)}, reminders every ${formatDuration(t.escalationGapMs)}. " +
        "Leaving starts a ${formatDuration(level.breakMs)} break ($skip)."
}
