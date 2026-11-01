package com.doomscrollguard.data

import com.doomscrollguard.engine.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun bedtimeOffByDefault() {
        assertFalse(Settings().isBedtime(23))
        assertEquals(Thresholds.DEFAULT, Settings().thresholdsAt(23))
    }

    @Test
    fun bedtimeWindowWrapsPastMidnight() {
        val s = Settings(bedtimeEnabled = true, bedtimeStartHour = 23, bedtimeEndHour = 6)
        assertTrue(s.isBedtime(23))
        assertTrue(s.isBedtime(0))
        assertTrue(s.isBedtime(5))
        assertFalse(s.isBedtime(6))
        assertFalse(s.isBedtime(12))
        assertFalse(s.isBedtime(22))
    }

    @Test
    fun bedtimeWindowWithinOneDay() {
        val s = Settings(bedtimeEnabled = true, bedtimeStartHour = 13, bedtimeEndHour = 15)
        assertFalse(s.isBedtime(12))
        assertTrue(s.isBedtime(13))
        assertTrue(s.isBedtime(14))
        assertFalse(s.isBedtime(15))
    }

    @Test
    fun bedtimeUsesStricterThresholds() {
        val s = Settings(bedtimeEnabled = true, bedtimeStartHour = 23, bedtimeEndHour = 6)
        assertEquals(Thresholds.DEFAULT.bedtime(), s.thresholdsAt(1))
        assertEquals(Thresholds.DEFAULT, s.thresholdsAt(12))
        assertEquals(Thresholds.DEMO.bedtime(), s.copy(demoMode = true).thresholdsAt(1))
    }

    @Test
    fun adaptiveFactorAppliesOnlyWhenEnabled() {
        val on = Settings(adaptiveEnabled = true)
        val off = Settings(adaptiveEnabled = false)
        assertEquals(Thresholds.DEFAULT.scaled(0.5), on.thresholdsAt(12, adaptiveFactor = 0.5))
        assertEquals(Thresholds.DEFAULT, off.thresholdsAt(12, adaptiveFactor = 0.5))
    }
}
