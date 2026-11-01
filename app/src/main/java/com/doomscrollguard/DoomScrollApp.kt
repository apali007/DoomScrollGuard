package com.doomscrollguard

import android.app.Application
import com.doomscrollguard.data.SettingsRepository
import com.doomscrollguard.data.StatsRepository
import com.doomscrollguard.data.db.StatsDatabase
import com.doomscrollguard.work.DailyRecapWorker

/** Holds the app-wide singletons shared by the UI and the accessibility service. */
class DoomScrollApp : Application() {
    val settings by lazy { SettingsRepository(this) }
    val stats by lazy { StatsRepository(StatsDatabase.create(this).statsDao()) }

    override fun onCreate() {
        super.onCreate()
        DailyRecapWorker.schedule(this)
    }
}
