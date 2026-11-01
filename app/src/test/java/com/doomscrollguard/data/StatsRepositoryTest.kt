package com.doomscrollguard.data

import com.doomscrollguard.data.db.InterventionEntity
import com.doomscrollguard.data.db.Outcome
import com.doomscrollguard.data.db.UsageEntity
import com.doomscrollguard.engine.MINUTE
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsRepositoryTest {

    private val today = LocalDate.of(2026, 11, 1)
    private fun day(back: Long) = today.minusDays(back).toString()

    private fun intervention(day: String, outcome: Outcome) = InterventionEntity(
        day = day,
        shownAtEpochMs = 0,
        pkg = "com.instagram.android",
        level = "NUDGE",
        outcome = outcome.name,
    )

    @Test
    fun summarizesToday() {
        val usage = listOf(
            UsageEntity(day(0), "com.instagram.android", 20 * MINUTE),
            UsageEntity(day(0), "com.google.android.youtube", 30 * MINUTE),
            UsageEntity(day(1), "com.instagram.android", 99 * MINUTE),
        )
        val interventions = listOf(
            intervention(day(0), Outcome.LEFT),
            intervention(day(0), Outcome.CONTINUED),
            intervention(day(0), Outcome.LEFT),
            intervention(day(1), Outcome.LEFT),
        )
        val s = StatsRepository.summarize(today, usage, interventions)

        assertEquals(50 * MINUTE, s.todayMs)
        assertEquals("com.google.android.youtube", s.todayByApp.first().pkg)
        assertEquals(3, s.todayInterventions)
        assertEquals(2, s.todayLeft)
        assertEquals(StatsSummary.HISTORY_DAYS, s.days.size)
        assertEquals(today, s.days.last().day)
        assertEquals(7, s.last7.size)
    }

    @Test
    fun streakCountsOnlyDaysSinceTrackingStarted() {
        // Tracking began 3 days ago; every day so far is under a 30 min goal.
        val usage = listOf(
            UsageEntity(day(3), "a", 10 * MINUTE),
            UsageEntity(day(2), "a", 25 * MINUTE),
            UsageEntity(day(0), "a", 5 * MINUTE),
        )
        val s = StatsRepository.summarize(today, usage, emptyList())
        assertEquals(4, s.streakUnder(30 * MINUTE)) // days 3,2,1 (zero),0
    }

    @Test
    fun streakBreaksOnDayOverGoal() {
        val usage = listOf(
            UsageEntity(day(3), "a", 10 * MINUTE),
            UsageEntity(day(2), "a", 90 * MINUTE),
            UsageEntity(day(1), "a", 10 * MINUTE),
            UsageEntity(day(0), "a", 10 * MINUTE),
        )
        val s = StatsRepository.summarize(today, usage, emptyList())
        assertEquals(2, s.streakUnder(30 * MINUTE))
    }

    @Test
    fun adaptiveCountsOnlyRecentPauseDecisions() {
        fun row(back: Long, level: String, outcome: Outcome) =
            intervention(day(back), outcome).copy(level = level)
        val interventions = listOf(
            row(0, "PAUSE", Outcome.CONTINUED),
            row(1, "INTENTION", Outcome.CONTINUED),
            row(2, "PAUSE", Outcome.CONTINUED),
            row(3, "PAUSE", Outcome.LEFT),
            row(0, "NUDGE", Outcome.IGNORED), // nudges don't count
            row(0, "PAUSE", Outcome.PENDING), // undecided doesn't count
            row(10, "PAUSE", Outcome.CONTINUED), // older than a week
        )
        val s = StatsRepository.summarize(today, emptyList(), interventions)
        assertEquals(3, s.recentContinued)
        assertEquals(4, s.recentDecided)
        assertEquals(0.75, s.adaptiveFactor, 0.0)
    }

    @Test
    fun noDataMeansNoStreak() {
        val s = StatsRepository.summarize(today, emptyList(), emptyList())
        assertEquals(0, s.streakUnder(30 * MINUTE))
        assertEquals(0L, s.todayMs)
    }
}
