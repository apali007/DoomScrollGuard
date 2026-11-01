package com.doomscrollguard.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object ServiceStatus {

    /** True if the user has DoomScroll Guard switched on in Accessibility settings. */
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, DoomScrollService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    /** Phone makers known to aggressively kill background services (see dontkillmyapp.com). */
    private val aggressiveOems = setOf(
        "samsung", "xiaomi", "redmi", "poco", "oppo", "realme", "vivo",
        "huawei", "honor", "oneplus", "meizu", "asus", "tecno", "infinix",
    )

    val manufacturer: String get() = Build.MANUFACTURER.lowercase()

    /** Whether to show battery-optimisation guidance on this phone. */
    fun needsBatteryHelp(context: Context): Boolean {
        if (manufacturer !in aggressiveOems) return false
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
