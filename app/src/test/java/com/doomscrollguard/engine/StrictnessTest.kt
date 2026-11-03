package com.doomscrollguard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictnessTest {

    @Test
    fun levelsGetStricterInEveryDimension() {
        val (g, b, s) = Strictness.entries
        assertTrue(g.thresholds.nudgeAfterMs > b.thresholds.nudgeAfterMs)
        assertTrue(b.thresholds.nudgeAfterMs > s.thresholds.nudgeAfterMs)
        assertTrue(g.thresholds.nudgeAfterGestures > b.thresholds.nudgeAfterGestures)
        assertTrue(b.thresholds.nudgeAfterGestures > s.thresholds.nudgeAfterGestures)
        assertTrue(g.pauseLockSeconds < b.pauseLockSeconds && b.pauseLockSeconds < s.pauseLockSeconds)
        assertTrue(g.intentionMinWords < b.intentionMinWords && b.intentionMinWords < s.intentionMinWords)
        assertTrue(g.breakMs < b.breakMs && b.breakMs < s.breakMs)
        assertNull(s.breakSkipAfterSeconds) // strict breaks can't be skipped
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
