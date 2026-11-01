package com.doomscrollguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doomscrollguard.engine.Thresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val demoMode: Boolean = false,
    val monitoredPackages: Set<String> = MonitoredApps.defaultEnabled,
    val disclosureAccepted: Boolean = false,
    val dailyGoalMinutes: Int = 30,
    val bedtimeEnabled: Boolean = false,
    val bedtimeStartHour: Int = 23,
    val bedtimeEndHour: Int = 6,
    val recapEnabled: Boolean = true,
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

    fun thresholdsAt(hour: Int): Thresholds {
        val base = if (demoMode) Thresholds.DEMO else Thresholds.DEFAULT
        return if (isBedtime(hour)) base.bedtime() else base
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            demoMode = prefs[DEMO_MODE] ?: defaults.demoMode,
            monitoredPackages = prefs[MONITORED] ?: defaults.monitoredPackages,
            disclosureAccepted = prefs[DISCLOSURE_ACCEPTED] ?: defaults.disclosureAccepted,
            dailyGoalMinutes = prefs[DAILY_GOAL] ?: defaults.dailyGoalMinutes,
            bedtimeEnabled = prefs[BEDTIME_ENABLED] ?: defaults.bedtimeEnabled,
            bedtimeStartHour = prefs[BEDTIME_START] ?: defaults.bedtimeStartHour,
            bedtimeEndHour = prefs[BEDTIME_END] ?: defaults.bedtimeEndHour,
            recapEnabled = prefs[RECAP_ENABLED] ?: defaults.recapEnabled,
        )
    }

    suspend fun setDemoMode(enabled: Boolean) = edit { it[DEMO_MODE] = enabled }

    suspend fun setMonitored(packageName: String, enabled: Boolean) = edit { prefs ->
        val current = prefs[MONITORED] ?: MonitoredApps.defaultEnabled
        prefs[MONITORED] = if (enabled) current + packageName else current - packageName
    }

    suspend fun acceptDisclosure() = edit { it[DISCLOSURE_ACCEPTED] = true }

    suspend fun setDailyGoal(minutes: Int) = edit { it[DAILY_GOAL] = minutes }

    suspend fun setBedtimeEnabled(enabled: Boolean) = edit { it[BEDTIME_ENABLED] = enabled }

    suspend fun setBedtimeHours(start: Int, end: Int) = edit {
        it[BEDTIME_START] = start.mod(24)
        it[BEDTIME_END] = end.mod(24)
    }

    suspend fun setRecapEnabled(enabled: Boolean) = edit { it[RECAP_ENABLED] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private companion object {
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val MONITORED = stringSetPreferencesKey("monitored_packages")
        val DISCLOSURE_ACCEPTED = booleanPreferencesKey("disclosure_accepted")
        val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
        val BEDTIME_ENABLED = booleanPreferencesKey("bedtime_enabled")
        val BEDTIME_START = intPreferencesKey("bedtime_start_hour")
        val BEDTIME_END = intPreferencesKey("bedtime_end_hour")
        val RECAP_ENABLED = booleanPreferencesKey("recap_enabled")
    }
}
