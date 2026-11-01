package com.doomscrollguard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveTest {

    @Test
    fun noChangeWithoutEnoughData() {
        assertEquals(1.0, Adaptive.factor(continued = 2, decided = 2), 0.0)
    }

    @Test
    fun usuallyLeavingKeepsThresholds() {
        assertEquals(1.0, Adaptive.factor(continued = 1, decided = 5), 0.0)
    }

    @Test
    fun oftenContinuingMakesRemindersSooner() {
        assertEquals(0.75, Adaptive.factor(continued = 3, decided = 5), 0.0)
    }

    @Test
    fun almostAlwaysContinuingHalvesThresholds() {
        assertEquals(0.5, Adaptive.factor(continued = 9, decided = 10), 0.0)
    }

    @Test
    fun scaledThresholdsNeverDropGesturesToZero() {
        val t = Thresholds(nudgeAfterMs = 1000, nudgeAfterGestures = 1, escalationGapMs = 1000, sessionResetMs = 1000)
        assertEquals(1, t.scaled(0.25).nudgeAfterGestures)
        assertEquals(250L, t.scaled(0.25).nudgeAfterMs)
        assertEquals(1000L, t.scaled(0.25).sessionResetMs)
    }
}
