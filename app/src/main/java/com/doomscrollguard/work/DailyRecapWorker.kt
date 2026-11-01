package com.doomscrollguard.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.doomscrollguard.DoomScrollApp
import com.doomscrollguard.R
import com.doomscrollguard.data.StatsSummary
import com.doomscrollguard.ui.MainActivity
import com.doomscrollguard.ui.overlay.formatDuration
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Posts a short evening summary of the day's feed time. */
class DailyRecapWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DoomScrollApp
        if (!app.settings.settings.first().recapEnabled) return Result.success()
        if (!canNotify(applicationContext)) return Result.success()

        val summary = app.stats.summaryOnce()
        postNotification(applicationContext, recapText(summary))
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_recap"
        private const val CHANNEL_ID = "daily_recap"
        private const val NOTIFICATION_ID = 1
        private val RECAP_TIME: LocalTime = LocalTime.of(21, 0)

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyRecapWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextRecap().toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun recapText(summary: StatsSummary): String = when {
            summary.todayMs == 0L -> "No doomscrolling today. That's a win 🌱"
            summary.todayLeft > 0 ->
                "Today: ${formatDuration(summary.todayMs)} in feeds · " +
                    "you stepped away ${summary.todayLeft} of ${summary.todayInterventions} times 👏"
            summary.todayInterventions > 0 ->
                "Today: ${formatDuration(summary.todayMs)} in feeds · ${summary.todayInterventions} reminders. Tomorrow's a fresh start."
            else -> "Today: ${formatDuration(summary.todayMs)} in feeds. Nice and balanced."
        }

        private fun delayUntilNextRecap(): Duration {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(RECAP_TIME)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        private fun postNotification(context: Context, text: String) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Daily recap", NotificationManager.IMPORTANCE_DEFAULT),
            )
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Your day in feeds")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
            @Suppress("MissingPermission") // checked in canNotify()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
