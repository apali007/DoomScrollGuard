package com.doomscrollguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doomscrollguard.engine.Thresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val monitoredPackages: Set<String> = emptySet(),
    val disclosureAccepted: Boolean = false,
    val dailyGoalMinutes: Int = 30,
    val bedtimeEnabled: Boolean = false,
    val bedtimeStartHour: Int = 23,
    val bedtimeEndHour: Int = 6,
    val recapEnabled: Boolean = true,
    val adaptiveEnabled: Boolean = true,
    val mindfulOpenEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    /** Set once the service has run, so we can warn if it later gets turned off. */
    val serviceWasEnabled: Boolean = false,
    val lastOffWarningEpochMs: Long = 0L,
) {
    /** True if [hour] (0-23) falls inside the bedtime window, which may wrap past midnight. */
    fun isBedtime(hour: Int): Boolean {
        if (!bedtimeEnabled || bedtimeStartHour == bedtimeEndHour) return false
        return if (bedtimeStartHour < bedtimeEndHour) {
            hour in bedtimeStartHour until bedtimeEndHour
        } else {
            hour >= bedtimeStartHour || hour < bedtimeEndHour
        }
    }

    /** [adaptiveFactor] comes from [com.doomscrollguard.engine.Adaptive]; ignored if adaptation is off. */
    fun thresholdsAt(hour: Int, adaptiveFactor: Double = 1.0): Thresholds {
        val base = Thresholds.DEFAULT
        val timed = if (isBedtime(hour)) base.bedtime() else base
        return if (adaptiveEnabled) timed.scaled(adaptiveFactor) else timed
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            monitoredPackages = prefs[MONITORED] ?: defaults.monitoredPackages,
            disclosureAccepted = prefs[DISCLOSURE_ACCEPTED] ?: defaults.disclosureAccepted,
            dailyGoalMinutes = prefs[DAILY_GOAL] ?: defaults.dailyGoalMinutes,
            bedtimeEnabled = prefs[BEDTIME_ENABLED] ?: defaults.bedtimeEnabled,
            bedtimeStartHour = prefs[BEDTIME_START] ?: defaults.bedtimeStartHour,
            bedtimeEndHour = prefs[BEDTIME_END] ?: defaults.bedtimeEndHour,
            recapEnabled = prefs[RECAP_ENABLED] ?: defaults.recapEnabled,
            adaptiveEnabled = prefs[ADAPTIVE_ENABLED] ?: defaults.adaptiveEnabled,
            mindfulOpenEnabled = prefs[MINDFUL_OPEN_ENABLED] ?: defaults.mindfulOpenEnabled,
            // Users from before onboarding existed already accepted the disclosure.
            onboardingDone = prefs[ONBOARDING_DONE] ?: prefs[DISCLOSURE_ACCEPTED] ?: false,
            serviceWasEnabled = prefs[SERVICE_WAS_ENABLED] ?: defaults.serviceWasEnabled,
            lastOffWarningEpochMs = prefs[LAST_OFF_WARNING] ?: defaults.lastOffWarningEpochMs,
        )
    }

    suspend fun setMonitored(packageName: String, enabled: Boolean) = edit { prefs ->
        val current = prefs[MONITORED] ?: emptySet()
        prefs[MONITORED] = if (enabled) current + packageName else current - packageName
    }

    /**
     * One-time migration from when Instagram/YouTube/Facebook etc. were watched by
     * default: keep only those defaults that are actually installed, so a feed app
     * installed later isn't silently watched. Choices the user made are kept.
     */
    suspend fun migrateOldDefaults(installed: Set<String>) = edit { prefs ->
        if (prefs[DEFAULTS_MIGRATED] == true) return@edit
        prefs[DEFAULTS_MIGRATED] = true
        val stored = prefs[MONITORED]
        val onboarded = prefs[ONBOARDING_DONE] ?: prefs[DISCLOSURE_ACCEPTED] ?: false
        when {
            stored != null -> prefs[MONITORED] = stored.filter {
                it !in MonitoredApps.knownPackages || it in installed
            }.toSet()
            // Never touched the list: they were relying on the old implicit defaults.
            onboarded -> prefs[MONITORED] = MonitoredApps.knownPackages intersect installed
        }
    }

    suspend fun acceptDisclosure() = edit { prefs ->
        prefs[DISCLOSURE_ACCEPTED] = true
        // Accepting mid-onboarding must not count as "onboarding done" via the
        // legacy fallback below, so record explicitly that it isn't finished yet.
        if (prefs[ONBOARDING_DONE] == null) prefs[ONBOARDING_DONE] = false
    }

    suspend fun setDailyGoal(minutes: Int) = edit { it[DAILY_GOAL] = minutes }

    suspend fun setBedtimeEnabled(enabled: Boolean) = edit { it[BEDTIME_ENABLED] = enabled }

    suspend fun setBedtimeHours(start: Int, end: Int) = edit {
        it[BEDTIME_START] = start.mod(24)
        it[BEDTIME_END] = end.mod(24)
    }

    suspend fun setRecapEnabled(enabled: Boolean) = edit { it[RECAP_ENABLED] = enabled }

    suspend fun setAdaptiveEnabled(enabled: Boolean) = edit { it[ADAPTIVE_ENABLED] = enabled }

    suspend fun setMindfulOpenEnabled(enabled: Boolean) = edit { it[MINDFUL_OPEN_ENABLED] = enabled }

    suspend fun completeOnboarding() = edit {
        it[ONBOARDING_DONE] = true
        it[DISCLOSURE_ACCEPTED] = true
    }

    suspend fun markServiceRan() = edit { it[SERVICE_WAS_ENABLED] = true }

    suspend fun markOffWarningShown(epochMs: Long) = edit { it[LAST_OFF_WARNING] = epochMs }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private companion object {
        val MONITORED = stringSetPreferencesKey("monitored_packages")
        val DISCLOSURE_ACCEPTED = booleanPreferencesKey("disclosure_accepted")
        val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
        val BEDTIME_ENABLED = booleanPreferencesKey("bedtime_enabled")
        val BEDTIME_START = intPreferencesKey("bedtime_start_hour")
        val BEDTIME_END = intPreferencesKey("bedtime_end_hour")
        val RECAP_ENABLED = booleanPreferencesKey("recap_enabled")
        val ADAPTIVE_ENABLED = booleanPreferencesKey("adaptive_enabled")
        val MINDFUL_OPEN_ENABLED = booleanPreferencesKey("mindful_open_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SERVICE_WAS_ENABLED = booleanPreferencesKey("service_was_enabled")
        val LAST_OFF_WARNING = longPreferencesKey("last_off_warning_epoch_ms")
        val DEFAULTS_MIGRATED = booleanPreferencesKey("defaults_migrated")
    }
}
