package com.doomscrollguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doomscrollguard.engine.Thresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val demoMode: Boolean = false,
    val monitoredPackages: Set<String> = MonitoredApps.defaultEnabled,
    val disclosureAccepted: Boolean = false,
) {
    val thresholds: Thresholds get() = if (demoMode) Thresholds.DEMO else Thresholds.DEFAULT
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            demoMode = prefs[DEMO_MODE] ?: false,
            monitoredPackages = prefs[MONITORED] ?: MonitoredApps.defaultEnabled,
            disclosureAccepted = prefs[DISCLOSURE_ACCEPTED] ?: false,
        )
    }

    suspend fun setDemoMode(enabled: Boolean) {
        context.dataStore.edit { it[DEMO_MODE] = enabled }
    }

    suspend fun setMonitored(packageName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[MONITORED] ?: MonitoredApps.defaultEnabled
            prefs[MONITORED] = if (enabled) current + packageName else current - packageName
        }
    }

    suspend fun acceptDisclosure() {
        context.dataStore.edit { it[DISCLOSURE_ACCEPTED] = true }
    }

    private companion object {
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val MONITORED = stringSetPreferencesKey("monitored_packages")
        val DISCLOSURE_ACCEPTED = booleanPreferencesKey("disclosure_accepted")
    }
}
