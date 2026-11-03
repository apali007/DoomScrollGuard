package com.doomscrollguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doomscrollguard.engine.Strictness
import com.doomscrollguard.engine.Thresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val monitoredPackages: Set<String> = emptySet(),
    val strictness: Strictness = Strictness.BALANCED,
    val disclosureAccepted: Boolean = false,
    val dailyGoalMinutes: Int = 30,
    val recapEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    /** Set once the service has run, so we can warn if it later gets turned off. */
    val serviceWasEnabled: Boolean = false,
    val lastOffWarningEpochMs: Long = 0L,
) {
    val thresholds: Thresholds get() = strictness.thresholds
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            monitoredPackages = prefs[MONITORED] ?: defaults.monitoredPackages,
            strictness = Strictness.fromName(prefs[STRICTNESS]),
            disclosureAccepted = prefs[DISCLOSURE_ACCEPTED] ?: defaults.disclosureAccepted,
            dailyGoalMinutes = prefs[DAILY_GOAL] ?: defaults.dailyGoalMinutes,
            recapEnabled = prefs[RECAP_ENABLED] ?: defaults.recapEnabled,
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

    suspend fun setStrictness(strictness: Strictness) = edit { it[STRICTNESS] = strictness.name }

    suspend fun acceptDisclosure() = edit { prefs ->
        prefs[DISCLOSURE_ACCEPTED] = true
        // Accepting mid-onboarding must not count as "onboarding done" via the
        // legacy fallback below, so record explicitly that it isn't finished yet.
        if (prefs[ONBOARDING_DONE] == null) prefs[ONBOARDING_DONE] = false
    }

    suspend fun setDailyGoal(minutes: Int) = edit { it[DAILY_GOAL] = minutes }

    suspend fun setRecapEnabled(enabled: Boolean) = edit { it[RECAP_ENABLED] = enabled }

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
        val STRICTNESS = stringPreferencesKey("strictness")
        val DISCLOSURE_ACCEPTED = booleanPreferencesKey("disclosure_accepted")
        val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
        val RECAP_ENABLED = booleanPreferencesKey("recap_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SERVICE_WAS_ENABLED = booleanPreferencesKey("service_was_enabled")
        val LAST_OFF_WARNING = longPreferencesKey("last_off_warning_epoch_ms")
        val DEFAULTS_MIGRATED = booleanPreferencesKey("defaults_migrated")
    }
}
