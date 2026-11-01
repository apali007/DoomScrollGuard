package com.doomscrollguard.engine

/** How hard an intervention pushes back. Higher levels are harder to dismiss. */
enum class InterventionLevel { NUDGE, PAUSE, INTENTION }

/**
 * Tunable rules for when to intervene. All durations are in milliseconds of
 * *active* time spent inside monitored apps.
 */
data class Thresholds(
    /** Show the first nudge after this much continuous feed time... */
    val nudgeAfterMs: Long,
    /** ...or after this many scroll gestures, whichever comes first. */
    val nudgeAfterGestures: Int,
    /** After an intervention, wait this long before escalating / repeating. */
    val escalationGapMs: Long,
    /** Being away from all monitored apps this long ends the session. */
    val sessionResetMs: Long,
) {
    /** Multiplies every trigger by [factor] (< 1 means interventions come sooner). */
    fun scaled(factor: Double): Thresholds {
        if (factor == 1.0) return this
        return copy(
            nudgeAfterMs = (nudgeAfterMs * factor).toLong(),
            nudgeAfterGestures = maxOf(1, (nudgeAfterGestures * factor).toInt()),
            escalationGapMs = (escalationGapMs * factor).toLong(),
        )
    }

    /** Stricter variant for late at night: everything kicks in twice as fast. */
    fun bedtime(): Thresholds = scaled(0.5)

    companion object {
        val DEFAULT = Thresholds(
            nudgeAfterMs = 10 * MINUTE,
            nudgeAfterGestures = 150,
            escalationGapMs = 5 * MINUTE,
            sessionResetMs = 2 * MINUTE,
        )

        /** Very low thresholds so the whole flow can be demoed in under a minute. */
        val DEMO = Thresholds(
            nudgeAfterMs = 30 * SECOND,
            nudgeAfterGestures = 8,
            escalationGapMs = 20 * SECOND,
            sessionResetMs = 15 * SECOND,
        )
    }
}

/** A snapshot of the current scrolling session, used for intervention copy. */
data class SessionStats(
    val currentPackage: String,
    val activeMs: Long,
    val gestures: Int,
    val distancePx: Long,
)

data class Intervention(val level: InterventionLevel, val stats: SessionStats)

const val SECOND = 1_000L
const val MINUTE = 60 * SECOND
