package com.example.privatecheck.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    fun scheduleAllworkers(context: Context) {
        schedulePenaltyCheck(context)
        scheduleDailyReminder(context)
    }

    fun schedulePenaltyCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<CheckPenaltyWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyPenaltyCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleDailyReminder(context: Context) {
        val now = LocalDateTime.now()
        var targetTime = now.withHour(21).withMinute(0).withSecond(0)
        if (now.isAfter(targetTime)) {
             targetTime = targetTime.plusDays(1)
        }
        val delayMinutes = Duration.between(now, targetTime).toMinutes()

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyReminder",
            ExistingPeriodicWorkPolicy.UPDATE, // Update to ensure correct timing if app restarts
            reminderRequest
        )
    }
}
