package com.doomscrollguard.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomscrollguard.data.Settings
import com.doomscrollguard.engine.SECOND
import com.doomscrollguard.ui.overlay.formatDuration

/** Which screen the user wants to see from the "How it works" list. */
enum class HowItWorksPreview { OPENING_QUESTION, NUDGE, PAUSE, INTENTION, BREAK }

/** Small card on the home screen that opens the full "How it works" page. */
@Composable
fun HowItWorksLink(settings: Settings, onOpen: () -> Unit) {
    val rules = settings.strictness
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("💡", fontSize = 26.sp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "When each reminder appears on ${rules.emoji} ${rules.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Full page explaining every reminder and exactly when it appears for the
 * chosen strictness level.
 */
@Composable
fun HowItWorksScreen(
    settings: Settings,
    onPreview: (HowItWorksPreview) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val rules = settings.strictness
    val now = rules.thresholds
    val nudge = formatDuration(now.nudgeAfterMs)
    val gap = formatDuration(now.escalationGapMs)
    val reset = formatDuration(now.sessionResetMs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("How it works", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "You're on ${rules.emoji} ${rules.label}. Only time in your watched apps counts. " +
                "Reminders get stronger the longer you keep scrolling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Step(
            emoji = "👋",
            title = "Opening question",
            whenText = "Every time you open a watched app after $reset or more away",
            body = "Asks what brings you there. \"Just browsing\" makes the first nudge come twice as fast; " +
                "\"Never mind\" takes you home.",
            onSee = { onPreview(HowItWorksPreview.OPENING_QUESTION) },
        )
        HorizontalDivider()
        Step(
            emoji = "⏳",
            title = "1. Nudge",
            whenText = "After $nudge of scrolling (or ${now.nudgeAfterGestures} swipes)",
            body = "A card in the middle of the screen shows how long you've been scrolling. It disappears on its own.",
            onSee = { onPreview(HowItWorksPreview.NUDGE) },
        )
        HorizontalDivider()
        Step(
            emoji = "🫁",
            title = "2. Pause",
            whenText = "$gap after the nudge",
            body = "A full-screen breathing pause. \"Continue\" unlocks after ${rules.pauseLockSeconds} seconds, " +
                "or tap \"Leave\" to go home.",
            onSee = { onPreview(HowItWorksPreview.PAUSE) },
        )
        HorizontalDivider()
        Step(
            emoji = "🧭",
            title = "3. Intention",
            whenText = "$gap after the pause, then every $gap",
            body = "Write at least ${rules.intentionMinWords} words about what you're looking for. " +
                "\"Continue\" unlocks after ${rules.intentionLockSeconds} seconds.",
            onSee = { onPreview(HowItWorksPreview.INTENTION) },
        )
        HorizontalDivider()
        Step(
            emoji = "🌿",
            title = "Break",
            whenText = "When you leave from a pause or intention",
            body = "Leaving with \"Leave\", Home or Recents locks your watched apps for " +
                "${formatDuration(rules.breakMs)}. Reopening one shows a countdown instead of the feed. " +
                "Come back after the break and you continue where you left off. " +
                (rules.breakSkipAfterSeconds?.let { "\"Open anyway\" appears after ${formatDuration(it * SECOND)}." }
                    ?: "On ${rules.label} it can't be skipped."),
            onSee = { onPreview(HowItWorksPreview.BREAK) },
        )
        HorizontalDivider()
        Step(
            emoji = "🎯",
            title = "Past your daily goal",
            whenText = "After ${settings.dailyGoalMinutes} min in feeds today",
            body = "Every new visit gets its first nudge after just 1 minute, whatever your level.",
            onSee = null,
        )

        HorizontalDivider()
        Text(
            "Leave your watched apps for $reset and everything starts over. Come back sooner and it " +
                "picks up where it left off. Locking your phone stops the clock.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Step(
    emoji: String,
    title: String,
    whenText: String,
    body: String,
    onSee: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text(emoji, fontSize = 26.sp, modifier = Modifier.padding(top = 2.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                whenText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onSee != null) TextButton(onClick = onSee) { Text("See it") }
    }
}
