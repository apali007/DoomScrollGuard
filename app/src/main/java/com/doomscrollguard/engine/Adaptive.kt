package com.doomscrollguard.engine

/**
 * Learns from how the user responds to the stronger interventions. If they
 * keep choosing "Continue", reminders start sooner; if they usually leave,
 * nothing changes. Never makes reminders *later* than the base thresholds.
 */
object Adaptive {
    /** Need at least this many decided pauses/intentions before adapting. */
    const val MIN_SAMPLES = 3

    /** Multiplier applied to thresholds (1.0 = unchanged, 0.5 = twice as fast). */
    fun factor(continued: Int, decided: Int): Double {
        if (decided < MIN_SAMPLES) return 1.0
        val continueRate = continued.toDouble() / decided
        return when {
            continueRate >= 0.8 -> 0.5
            continueRate >= 0.5 -> 0.75
            else -> 1.0
        }
    }
}
