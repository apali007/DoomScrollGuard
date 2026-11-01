package com.doomscrollguard.engine

import kotlin.math.abs

/**
 * Pure decision logic: tracks a "feed session" across all monitored apps and
 * decides when to intervene. Has no Android dependencies so it is fully unit
 * testable; callers pass in a monotonic clock value for every call.
 *
 * A session spans all monitored apps (hopping Instagram -> TikTok is still the
 * same doomscroll) and only ends after [Thresholds.sessionResetMs] away.
 */
class SessionEngine(
    thresholds: Thresholds = Thresholds.DEFAULT,
    private val aggregator: GestureAggregator = GestureAggregator(),
) {
    var thresholds: Thresholds = thresholds
        private set

    private var session: Session? = null

    /** Package of the monitored app currently on screen, or null. */
    var foregroundPackage: String? = null
        private set

    val isActive: Boolean get() = foregroundPackage != null

    /** Increments every time a fresh session starts, so callers can react to new sessions. */
    var sessionCount: Int = 0
        private set

    /**
     * Makes the first nudge of the current session come sooner (or later) by
     * [factor], e.g. 0.5 when the user says they're "just browsing".
     */
    fun scaleCurrentSessionNudge(factor: Double) {
        session?.nudgeScale = factor
    }

    fun updateThresholds(newThresholds: Thresholds) {
        thresholds = newThresholds
    }

    /**
     * Reports which app is on screen. Pass null (or a non-monitored package with
     * [monitored] = false) when the user leaves monitored apps or the screen turns off.
     */
    fun onForeground(pkg: String?, monitored: Boolean, now: Long) {
        val enteringPkg = if (monitored) pkg else null
        val s = session

        if (enteringPkg == null) {
            if (foregroundPackage != null && s != null) {
                s.pauseAt(now)
            }
            foregroundPackage = null
            aggregator.reset()
            return
        }

        if (foregroundPackage == null) {
            val leftAt = s?.leftAt
            if (s == null || (leftAt != null && now - leftAt >= thresholds.sessionResetMs)) {
                session = Session(activeSince = now)
                sessionCount++
            } else {
                s.resumeAt(now)
            }
        }
        if (foregroundPackage != enteringPkg) aggregator.reset()
        foregroundPackage = enteringPkg
    }

    /**
     * Distance credited to a gesture whose events carry no scroll delta (common
     * for Reels/Shorts-style pagers). Roughly one screen swipe.
     */
    var fallbackGesturePx: Int = 0

    /** Reports a raw scroll event from the foreground monitored app. */
    fun onScroll(deltaPx: Int, now: Long): Intervention? {
        val s = session ?: return null
        if (!isActive) return null
        val newGesture = aggregator.onScrollEvent(now)
        if (newGesture) s.gestures++
        s.distancePx += when {
            deltaPx != 0 -> abs(deltaPx)
            newGesture -> fallbackGesturePx
            else -> 0
        }
        return check(now)
    }

    /** Called periodically while active so time-based rules fire without scrolling. */
    fun tick(now: Long): Intervention? = check(now)

    fun currentStats(now: Long): SessionStats? {
        val s = session ?: return null
        return SessionStats(
            currentPackage = foregroundPackage ?: "",
            activeMs = s.activeMs(now),
            gestures = s.gestures,
            distancePx = s.distancePx,
        )
    }

    private fun check(now: Long): Intervention? {
        val s = session ?: return null
        if (!isActive) return null
        val active = s.activeMs(now)

        val level = when (val last = s.lastLevel) {
            null -> {
                val due = active >= thresholds.nudgeAfterMs * s.nudgeScale ||
                    s.gestures >= thresholds.nudgeAfterGestures * s.nudgeScale
                if (due) InterventionLevel.NUDGE else null
            }
            else -> {
                val due = active - s.activeAtLastIntervention >= thresholds.escalationGapMs
                if (due) escalate(last) else null
            }
        } ?: return null

        s.lastLevel = level
        s.activeAtLastIntervention = active
        return Intervention(level, currentStats(now)!!)
    }

    private fun escalate(from: InterventionLevel): InterventionLevel {
        val all = InterventionLevel.entries
        return all[minOf(from.ordinal + 1, all.lastIndex)]
    }

    private class Session(var activeSince: Long?) {
        var accumulatedMs = 0L
        var leftAt: Long? = null
        var gestures = 0
        var distancePx = 0L
        var lastLevel: InterventionLevel? = null
        var activeAtLastIntervention = 0L
        var nudgeScale = 1.0

        fun activeMs(now: Long): Long = accumulatedMs + (activeSince?.let { now - it } ?: 0L)

        fun pauseAt(now: Long) {
            activeSince?.let { accumulatedMs += now - it }
            activeSince = null
            leftAt = now
        }

        fun resumeAt(now: Long) {
            activeSince = now
            leftAt = null
        }
    }
}
