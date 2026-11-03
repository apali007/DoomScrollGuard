package com.doomscrollguard.data

import com.doomscrollguard.engine.Strictness
import com.doomscrollguard.engine.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    @Test
    fun defaultsToBalancedThresholds() {
        assertEquals(Strictness.BALANCED, Settings().strictness)
        assertEquals(Thresholds.DEFAULT, Settings().thresholds)
    }

    @Test
    fun thresholdsFollowStrictness() {
        Strictness.entries.forEach { level ->
            assertEquals(level.thresholds, Settings(strictness = level).thresholds)
        }
    }
}
