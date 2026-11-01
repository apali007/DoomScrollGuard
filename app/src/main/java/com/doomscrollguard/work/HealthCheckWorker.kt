package com.doomscrollguard.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.doomscrollguard.DoomScrollApp
import com.doomscrollguard.R
import com.doomscrollguard.service.ServiceStatus
import com.doomscrollguard.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Android (or the user, by accident) can switch the accessibility service off,
 * e.g. after a force-stop. If protection was on before and is now off, nudge
 * the user, at most once a day.
 */
class HealthCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DoomScrollApp
        val settings = app.settings.settings.first()
        if (!settings.serviceWasEnabled) return Result.success()
        if (ServiceStatus.isEnabled(applicationContext)) return Result.success()

        val now = System.currentTimeMillis()
        if (now - settings.lastOffWarningEpochMs < WARN_INTERVAL_MS) return Result.success()
        if (!DailyRecapWorker.canNotify(applicationContext)) return Result.success()

        postWarning(applicationContext)
        app.settings.markOffWarningShown(now)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "health_check"
        private const val CHANNEL_ID = "protection_status"
        private const val NOTIFICATION_ID = 2
        private val WARN_INTERVAL_MS = TimeUnit.HOURS.toMillis(24)

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthCheckWorker>(3, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun postWarning(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Protection status", NotificationManager.IMPORTANCE_DEFAULT),
            )
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val text = "DoomScroll Guard was switched off, so feeds aren't being watched. Tap to turn it back on."
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Protection is off")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
            @Suppress("MissingPermission") // checked via canNotify()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
