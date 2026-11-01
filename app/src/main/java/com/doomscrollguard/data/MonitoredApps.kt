package com.doomscrollguard.data

/** Feed apps we know how to watch. Must stay in sync with <queries> in the manifest. */
data class KnownApp(val packageName: String, val label: String)

object MonitoredApps {
    val known = listOf(
        KnownApp("com.instagram.android", "Instagram"),
        KnownApp("com.google.android.youtube", "YouTube"),
        KnownApp("com.facebook.katana", "Facebook"),
        KnownApp("com.zhiliaoapp.musically", "TikTok"),
        KnownApp("com.ss.android.ugc.trill", "TikTok"),
        KnownApp("com.twitter.android", "X"),
        KnownApp("com.reddit.frontpage", "Reddit"),
        KnownApp("com.snapchat.android", "Snapchat"),
    )

    val defaultEnabled: Set<String> = known.map { it.packageName }.toSet()

    fun labelFor(packageName: String): String =
        known.firstOrNull { it.packageName == packageName }?.label ?: "this app"
}
