package com.doomscrollguard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {

    private val thresholds = Thresholds(
        nudgeAfterMs = 10 * MINUTE,
        nudgeAfterGestures = 5,
        escalationGapMs = 5 * MINUTE,
        sessionResetMs = 2 * MINUTE,
    )

    private fun engine() = SessionEngine(thresholds)

    private val insta = "com.instagram.android"
    private val tiktok = "com.zhiliaoapp.musically"
    private val launcher = "com.google.android.apps.nexuslauncher"

    /** Simulates [count] distinct swipes starting at [start], one per second. */
    private fun SessionEngine.swipe(count: Int, start: Long): List<Intervention?> =
        (0 until count).map { onScroll(deltaPx = 500, now = start + it * SECOND) }

    @Test
    fun nothingHappensOutsideMonitoredApps() {
        val e = engine()
        e.onForeground(launcher, monitored = false, now = 0)
        assertFalse(e.isActive)
        assertNull(e.onScroll(500, now = 100))
        assertNull(e.tick(now = 60 * MINUTE))
    }

    @Test
    fun nudgeFiresAfterTimeThreshold() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        assertNull(e.tick(now = 10 * MINUTE - 1))
        val i = e.tick(now = 10 * MINUTE)
        assertEquals(InterventionLevel.NUDGE, i?.level)
        assertEquals(insta, i?.stats?.currentPackage)
    }

    @Test
    fun nudgeFiresAfterGestureThreshold() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        val results = e.swipe(5, start = 0)
        assertNull(results[3])
        assertEquals(InterventionLevel.NUDGE, results[4]?.level)
        assertEquals(5, results[4]?.stats?.gestures)
        assertEquals(2500L, results[4]?.stats?.distancePx)
    }

    @Test
    fun nudgeIsNotRepeatedImmediately() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        assertNotNull(e.tick(now = 10 * MINUTE))
        assertNull(e.tick(now = 10 * MINUTE + SECOND))
        assertNull(e.tick(now = 15 * MINUTE - 1))
    }

    @Test
    fun escalatesNudgePauseIntentionThenRepeatsIntention() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        assertEquals(InterventionLevel.NUDGE, e.tick(now = 10 * MINUTE)?.level)
        assertEquals(InterventionLevel.PAUSE, e.tick(now = 15 * MINUTE)?.level)
        assertNull(e.tick(now = 19 * MINUTE))
        assertEquals(InterventionLevel.INTENTION, e.tick(now = 20 * MINUTE)?.level)
        assertEquals(InterventionLevel.INTENTION, e.tick(now = 25 * MINUTE)?.level)
    }

    @Test
    fun timeAwayDoesNotCountTowardActiveTime() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        e.onForeground(launcher, monitored = false, now = 6 * MINUTE)
        // Back within the reset window: session continues, but the minute away isn't counted.
        e.onForeground(insta, monitored = true, now = 7 * MINUTE)
        assertNull(e.tick(now = 10 * MINUTE))
        assertEquals(InterventionLevel.NUDGE, e.tick(now = 11 * MINUTE)?.level)
    }

    @Test
    fun longBreakResetsSession() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        assertNotNull(e.tick(now = 10 * MINUTE))
        e.onForeground(null, monitored = false, now = 10 * MINUTE)
        e.onForeground(insta, monitored = true, now = 12 * MINUTE)
        // Fresh session: no escalation 5 minutes later, nudge again after 10.
        assertNull(e.tick(now = 17 * MINUTE))
        assertEquals(InterventionLevel.NUDGE, e.tick(now = 22 * MINUTE)?.level)
    }

    @Test
    fun quickReturnAfterLeavingKeepsEscalation() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        assertEquals(InterventionLevel.NUDGE, e.tick(now = 10 * MINUTE)?.level)
        e.onForeground(launcher, monitored = false, now = 10 * MINUTE)
        e.onForeground(insta, monitored = true, now = 10 * MINUTE + 30 * SECOND)
        assertEquals(
            InterventionLevel.PAUSE,
            e.tick(now = 15 * MINUTE + 30 * SECOND)?.level,
        )
    }

    @Test
    fun switchingBetweenMonitoredAppsIsOneSession() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        e.onForeground(tiktok, monitored = true, now = 6 * MINUTE)
        val i = e.tick(now = 10 * MINUTE)
        assertEquals(InterventionLevel.NUDGE, i?.level)
        assertEquals(tiktok, i?.stats?.currentPackage)
        assertEquals(10 * MINUTE, i?.stats?.activeMs)
    }

    @Test
    fun screenOffStopsTheClock() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        e.onForeground(null, monitored = false, now = 5 * MINUTE)
        assertFalse(e.isActive)
        assertNull(e.tick(now = 60 * MINUTE))
    }

    @Test
    fun sessionCountIncrementsOnlyForFreshSessions() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        assertEquals(1, e.sessionCount)
        e.onForeground(launcher, monitored = false, now = MINUTE)
        e.onForeground(insta, monitored = true, now = MINUTE + 30 * SECOND)
        assertEquals(1, e.sessionCount) // quick return: same session
        e.onForeground(launcher, monitored = false, now = 2 * MINUTE)
        e.onForeground(insta, monitored = true, now = 5 * MINUTE)
        assertEquals(2, e.sessionCount)
    }

    @Test
    fun justBrowsingMakesFirstNudgeSooner() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        e.scaleCurrentSessionNudge(0.5)
        assertNull(e.tick(now = 5 * MINUTE - 1))
        assertEquals(InterventionLevel.NUDGE, e.tick(now = 5 * MINUTE)?.level)
    }

    @Test
    fun thresholdsCanBeSwappedLive() {
        val e = engine()
        e.onForeground(insta, monitored = true, now = 0)
        assertNull(e.tick(now = 5 * MINUTE))
        e.updateThresholds(Strictness.STRICT.thresholds) // user switches level mid-session
        assertTrue(e.tick(now = 5 * MINUTE + SECOND)?.level == InterventionLevel.NUDGE)
    }
}
