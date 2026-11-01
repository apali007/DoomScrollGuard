package com.doomscrollguard.data

import com.doomscrollguard.data.db.InterventionEntity
import com.doomscrollguard.data.db.Outcome
import com.doomscrollguard.data.db.StatsDao
import com.doomscrollguard.data.db.UsageEntity
import com.doomscrollguard.engine.InterventionLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class DayTotal(val day: LocalDate, val activeMs: Long)

data class AppTotal(val pkg: String, val activeMs: Long)

/** Everything the stats screen and the daily recap need. */
data class StatsSummary(
    val today: LocalDate,
    val todayMs: Long,
    val todayByApp: List<AppTotal>,
    val todayInterventions: Int,
    val todayLeft: Int,
    /** Oldest first, always [HISTORY_DAYS] entries ending today. */
    val days: List<DayTotal>,
    /** First day we have any data for, or null if none yet. */
    val firstDataDay: LocalDate?,
) {
    val last7: List<DayTotal> get() = days.takeLast(7)

    /** Consecutive days (ending today) at or under [goalMs], counting only days since tracking began. */
    fun streakUnder(goalMs: Long): Int {
        val first = firstDataDay ?: return 0
        return days.reversed()
            .takeWhile { !it.day.isBefore(first) && it.activeMs <= goalMs }
            .count()
    }

    companion object {
        const val HISTORY_DAYS = 30
    }
}

class StatsRepository(private val dao: StatsDao) {

    suspend fun addUsage(pkg: String, ms: Long, day: LocalDate = LocalDate.now()) {
        if (ms <= 0) return
        dao.addUsage(day.toString(), pkg, ms)
    }

    suspend fun logIntervention(pkg: String, level: InterventionLevel): Long =
        dao.insertIntervention(
            InterventionEntity(
                day = LocalDate.now().toString(),
                shownAtEpochMs = System.currentTimeMillis(),
                pkg = pkg,
                level = level.name,
                outcome = Outcome.PENDING.name,
            ),
        )

    suspend fun setOutcome(id: Long, outcome: Outcome, reason: String? = null) {
        dao.setOutcome(id, outcome.name, reason)
    }

    fun summary(today: LocalDate = LocalDate.now()): Flow<StatsSummary> {
        val from = today.minusDays(StatsSummary.HISTORY_DAYS - 1L).toString()
        return combine(dao.usageSince(from), dao.interventionsSince(from)) { usage, interventions ->
            summarize(today, usage, interventions)
        }
    }

    suspend fun summaryOnce(today: LocalDate = LocalDate.now()): StatsSummary {
        val from = today.minusDays(StatsSummary.HISTORY_DAYS - 1L).toString()
        return summarize(today, dao.usageSinceOnce(from), dao.interventionsSinceOnce(from))
    }

    companion object {
        fun summarize(
            today: LocalDate,
            usage: List<UsageEntity>,
            interventions: List<InterventionEntity>,
        ): StatsSummary {
            val todayKey = today.toString()
            val byDay = usage.groupBy { it.day }.mapValues { (_, rows) -> rows.sumOf { it.activeMs } }
            val days = (StatsSummary.HISTORY_DAYS - 1 downTo 0).map { back ->
                val d = today.minusDays(back.toLong())
                DayTotal(d, byDay[d.toString()] ?: 0L)
            }
            val todayRows = usage.filter { it.day == todayKey }
            val todayInterventions = interventions.filter { it.day == todayKey }
            val firstDay = (usage.map { it.day } + interventions.map { it.day }).minOrNull()

            return StatsSummary(
                today = today,
                todayMs = todayRows.sumOf { it.activeMs },
                todayByApp = todayRows
                    .groupBy { it.pkg }
                    .map { (pkg, rows) -> AppTotal(pkg, rows.sumOf { it.activeMs }) }
                    .sortedByDescending { it.activeMs },
                todayInterventions = todayInterventions.size,
                todayLeft = todayInterventions.count { it.outcome == Outcome.LEFT.name },
                days = days,
                firstDataDay = firstDay?.let(LocalDate::parse),
            )
        }
    }
}
