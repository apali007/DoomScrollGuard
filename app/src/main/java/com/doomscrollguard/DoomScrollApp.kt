package com.doomscrollguard

import android.app.Application
import com.doomscrollguard.data.MonitoredApps
import com.doomscrollguard.data.SettingsRepository
import com.doomscrollguard.data.StatsRepository
import com.doomscrollguard.data.db.StatsDatabase
import com.doomscrollguard.work.DailyRecapWorker
import com.doomscrollguard.work.HealthCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Holds the app-wide singletons shared by the UI and the accessibility service. */
class DoomScrollApp : Application() {
    val settings by lazy { SettingsRepository(this) }
    val stats by lazy { StatsRepository(StatsDatabase.create(this).statsDao()) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        DailyRecapWorker.schedule(this)
        HealthCheckWorker.schedule(this)
        appScope.launch {
            val installed = MonitoredApps.installedLaunchable(packageManager, packageName)
                .map { it.packageName }
                .toSet()
            settings.migrateOldDefaults(installed)
        }
    }
}
