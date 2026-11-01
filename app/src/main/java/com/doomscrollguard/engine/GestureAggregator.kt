package com.doomscrollguard.engine

/**
 * A single fling produces dozens of scroll events. This groups events that
 * arrive close together into one "gesture" so counts reflect what the user
 * actually did (one swipe = one gesture), including Reels/Shorts page swipes.
 */
class GestureAggregator(private val gapMs: Long = DEFAULT_GAP_MS) {
    private var lastEventAt: Long? = null

    /** Records a scroll event and returns true if it starts a new gesture. */
    fun onScrollEvent(now: Long): Boolean {
        val last = lastEventAt
        lastEventAt = now
        return last == null || now - last >= gapMs
    }

    fun reset() {
        lastEventAt = null
    }

    companion object {
        const val DEFAULT_GAP_MS = 400L
    }
}
