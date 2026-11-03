package com.doomscrollguard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictnessTest {

    @Test
    fun levelsGetStricterInEveryDimension() {
        Strictness.entries.zipWithNext().forEach { (looser, stricter) ->
            val msg = "$looser -> $stricter"
            assertTrue(msg, looser.thresholds.nudgeAfterMs > stricter.thresholds.nudgeAfterMs)
            assertTrue(msg, looser.thresholds.nudgeAfterGestures > stricter.thresholds.nudgeAfterGestures)
            assertTrue(msg, looser.thresholds.escalationGapMs >= stricter.thresholds.escalationGapMs)
            assertTrue(msg, looser.pauseLockSeconds < stricter.pauseLockSeconds)
            assertTrue(msg, looser.intentionLockSeconds < stricter.intentionLockSeconds)
            assertTrue(msg, looser.intentionMinWords < stricter.intentionMinWords)
            assertTrue(msg, looser.breakMs <= stricter.breakMs)
            assertTrue(msg, looser.breakSkipAfterSeconds!! < stricter.breakSkipAfterSeconds!!)
        }
    }

    @Test
    fun strictAndHardcoreBreaksAsRequested() {
        assertEquals(5 * MINUTE, Strictness.STRICT.breakMs)
        assertEquals(60, Strictness.STRICT.breakSkipAfterSeconds)
        assertEquals(5 * MINUTE, Strictness.HARDCORE.breakMs)
        assertEquals(120, Strictness.HARDCORE.breakSkipAfterSeconds)
    }

    @Test
    fun hardcoreTiming() {
        assertEquals(3 * MINUTE, Strictness.HARDCORE.thresholds.nudgeAfterMs)
        assertEquals(2 * MINUTE, Strictness.HARDCORE.thresholds.escalationGapMs)
    }

    @Test
    fun balancedMatchesDefaults() {
        assertEquals(Thresholds.DEFAULT, Strictness.BALANCED.thresholds)
    }

    @Test
    fun unknownNameFallsBackToBalanced() {
        assertEquals(Strictness.BALANCED, Strictness.fromName(null))
        assertEquals(Strictness.BALANCED, Strictness.fromName("NOPE"))
        assertEquals(Strictness.STRICT, Strictness.fromName("STRICT"))
    }
}

class BreakTimerTest {

    @Test
    fun breakRunsForItsDuration() {
        val t = BreakTimer()
        assertFalse(t.isActive(0))
        t.start(now = 1_000, durationMs = 5 * MINUTE)
        assertTrue(t.isActive(1_000))
        assertTrue(t.isActive(1_000 + 5 * MINUTE - 1))
        assertFalse(t.isActive(1_000 + 5 * MINUTE))
        assertEquals(60_000L, t.remainingMs(1_000 + 4 * MINUTE))
    }

    @Test
    fun startingAgainNeverShortensABreak() {
        val t = BreakTimer()
        t.start(now = 0, durationMs = 15 * MINUTE)
        t.start(now = MINUTE, durationMs = 2 * MINUTE)
        assertTrue(t.isActive(10 * MINUTE))
    }

    @Test
    fun cancelEndsBreak() {
        val t = BreakTimer()
        t.start(now = 0, durationMs = 5 * MINUTE)
        t.cancel()
        assertFalse(t.isActive(1))
        assertEquals(0L, t.remainingMs(1))
    }
}
