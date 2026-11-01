package com.doomscrollguard.data

import android.content.Intent
import android.content.pm.PackageManager

/** Common feed apps, listed first in the picker if installed. Nothing is watched by default. */
data class KnownApp(val packageName: String, val label: String)

/** A launchable app installed on the phone. */
data class InstalledApp(val packageName: String, val label: String)

object MonitoredApps {
    val known = listOf(
        KnownApp("com.instagram.android", "Instagram"),
        KnownApp("com.instagram.lite", "Instagram Lite"),
        KnownApp("com.google.android.youtube", "YouTube"),
        KnownApp("com.facebook.katana", "Facebook"),
        KnownApp("com.facebook.lite", "Facebook Lite"),
        KnownApp("com.zhiliaoapp.musically", "TikTok"),
        KnownApp("com.ss.android.ugc.trill", "TikTok"),
        KnownApp("com.twitter.android", "X"),
        KnownApp("com.reddit.frontpage", "Reddit"),
        KnownApp("com.snapchat.android", "Snapchat"),
    )

    val knownPackages: Set<String> = known.map { it.packageName }.toSet()

    /** Human-readable name for any package, preferring our known labels. */
    fun labelFor(packageName: String, pm: PackageManager? = null): String {
        known.firstOrNull { it.packageName == packageName }?.let { return it.label }
        if (pm != null) {
            runCatching {
                return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }
        }
        return "this app"
    }

    /** All launchable apps on the phone (visible via the LAUNCHER <queries> entry). */
    fun installedLaunchable(pm: PackageManager, selfPackage: String): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != selfPackage }
            .map { InstalledApp(it, labelFor(it, pm)) }
            .sortedBy { it.label.lowercase() }
    }
}
