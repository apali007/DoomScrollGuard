package com.doomscrollguard.engine

/** How hard the app pushes back. Chosen by the user; Balanced is the default. */
enum class Strictness(
    val label: String,
    val emoji: String,
    val thresholds: Thresholds,
    /** Seconds "Continue" stays locked on the pause screen. */
    val pauseLockSeconds: Int,
    /** Seconds "Continue" stays locked on the intention screen. */
    val intentionLockSeconds: Int,
    /** Words the user must write on the intention screen. */
    val intentionMinWords: Int,
    /** How long watched apps stay locked after leaving from a pause/intention. */
    val breakMs: Long,
    /** Seconds before "Open anyway" appears on the break screen; null = can't skip. */
    val breakSkipAfterSeconds: Int?,
) {
    GENTLE(
        label = "Gentle",
        emoji = "🌱",
        thresholds = Thresholds(
            nudgeAfterMs = 15 * MINUTE,
            nudgeAfterGestures = 200,
            escalationGapMs = 5 * MINUTE,
            sessionResetMs = 2 * MINUTE,
        ),
        pauseLockSeconds = 5,
        intentionLockSeconds = 10,
        intentionMinWords = 2,
        breakMs = 2 * MINUTE,
        breakSkipAfterSeconds = 10,
    ),
    BALANCED(
        label = "Balanced",
        emoji = "⚖️",
        thresholds = Thresholds.DEFAULT,
        pauseLockSeconds = 10,
        intentionLockSeconds = 15,
        intentionMinWords = 3,
        breakMs = 5 * MINUTE,
        breakSkipAfterSeconds = 30,
    ),
    STRICT(
        label = "Strict",
        emoji = "🔒",
        thresholds = Thresholds(
            nudgeAfterMs = 5 * MINUTE,
            nudgeAfterGestures = 75,
            escalationGapMs = 3 * MINUTE,
            sessionResetMs = 2 * MINUTE,
        ),
        pauseLockSeconds = 20,
        intentionLockSeconds = 30,
        intentionMinWords = 5,
        breakMs = 5 * MINUTE,
        breakSkipAfterSeconds = 60,
    ),
    HARDCORE(
        label = "Hardcore",
        emoji = "🔥",
        thresholds = Thresholds(
            nudgeAfterMs = 1 * MINUTE,
            nudgeAfterGestures = 20,
            escalationGapMs = 1 * MINUTE,
            sessionResetMs = 2 * MINUTE,
        ),
        pauseLockSeconds = 30,
        intentionLockSeconds = 45,
        intentionMinWords = 7,
        breakMs = 5 * MINUTE,
        breakSkipAfterSeconds = 120,
    );

    companion object {
        fun fromName(name: String?): Strictness = entries.firstOrNull { it.name == name } ?: BALANCED
    }
}

/**
 * A lock on all watched apps after the user chose to leave. Reopening one
 * during the break shows the break screen instead of the feed. Pure logic so
 * it can be unit tested; callers pass a monotonic clock.
 */
class BreakTimer {
    private var endsAt: Long = 0L

    fun start(now: Long, durationMs: Long) {
        // Never shorten a break that's already running.
        endsAt = maxOf(endsAt, now + durationMs)
    }

    fun isActive(now: Long): Boolean = now < endsAt

    fun remainingMs(now: Long): Long = (endsAt - now).coerceAtLeast(0L)

    /** Monotonic time the break ends, for countdown displays. */
    val endsAtMs: Long get() = endsAt

    fun cancel() {
        endsAt = 0L
    }
}
