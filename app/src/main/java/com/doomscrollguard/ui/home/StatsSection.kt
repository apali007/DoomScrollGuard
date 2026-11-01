package com.doomscrollguard.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doomscrollguard.data.MonitoredApps
import com.doomscrollguard.data.StatsSummary
import com.doomscrollguard.engine.MINUTE
import com.doomscrollguard.ui.overlay.formatDuration
import com.doomscrollguard.ui.theme.Leaf
import java.time.format.TextStyle
import java.util.Locale

private val OverGoal = Color(0xFFE5735C)

@Composable
fun StatsSection(summary: StatsSummary?, goalMinutes: Int) {
    val goalMs = goalMinutes * MINUTE
    SectionCard(title = "Today") {
        if (summary == null) {
            Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }

        val over = summary.todayMs > goalMs
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatDuration(summary.todayMs),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (over) OverGoal else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "  of ${goalMinutes} min goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        LinearProgressIndicator(
            progress = { (summary.todayMs.toFloat() / goalMs).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (over) OverGoal else Leaf,
            drawStopIndicator = {},
        )

        if (summary.todayByApp.isNotEmpty()) {
            val pm = LocalContext.current.packageManager
            summary.todayByApp.take(4).forEach { app ->
                Row(Modifier.fillMaxWidth()) {
                    Text(MonitoredApps.labelFor(app.pkg, pm), modifier = Modifier.weight(1f))
                    Text(formatDuration(app.activeMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (summary.todayInterventions > 0) {
            Text(
                "You stepped away ${summary.todayLeft} of ${summary.todayInterventions} times you were reminded" +
                    if (summary.todayLeft > 0) " 👏" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val streak = summary.streakUnder(goalMs)
        if (streak >= 2) {
            Text("🔥 $streak days in a row within your goal", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (summary != null) {
        SectionCard(title = "Last 7 days") {
            WeekChart(summary, goalMs)
        }
    }
}

@Composable
private fun WeekChart(summary: StatsSummary, goalMs: Long) {
    val days = summary.last7
    val maxMs = maxOf(days.maxOf { it.activeMs }, goalMs, 1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (day.activeMs > 0) {
                    Text(
                        (day.activeMs / MINUTE).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .fillMaxHeight((day.activeMs.toFloat() / maxMs).coerceIn(0.02f, 1f))
                        .background(
                            if (day.activeMs > goalMs) OverGoal else Leaf,
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        ),
                )
                Text(
                    day.day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.day == summary.today) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
    Text(
        "Minutes per day. Green = within your goal.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
