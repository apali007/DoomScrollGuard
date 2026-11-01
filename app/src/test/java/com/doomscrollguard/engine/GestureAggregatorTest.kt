package com.doomscrollguard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureAggregatorTest {

    @Test
    fun burstOfEventsCountsAsOneGesture() {
        val agg = GestureAggregator(gapMs = 400)
        val starts = (0 until 50).count { i -> agg.onScrollEvent(now = i * 16L) }
        assertEquals(1, starts)
    }

    @Test
    fun separateSwipesCountSeparately() {
        val agg = GestureAggregator(gapMs = 400)
        assertTrue(agg.onScrollEvent(0))
        assertFalse(agg.onScrollEvent(100))
        assertTrue(agg.onScrollEvent(600))
        assertTrue(agg.onScrollEvent(1200))
    }

    @Test
    fun resetStartsFreshGesture() {
        val agg = GestureAggregator(gapMs = 400)
        agg.onScrollEvent(0)
        agg.reset()
        assertTrue(agg.onScrollEvent(10))
    }
}
