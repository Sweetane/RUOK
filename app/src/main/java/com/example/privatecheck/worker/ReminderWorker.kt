package com.example.privatecheck.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.privatecheck.MainActivity
import com.example.privatecheck.R
import com.example.privatecheck.data.DataStoreRepository
import com.example.privatecheck.logic.CheckInManager
import kotlinx.coroutines.flow.first

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = DataStoreRepository(applicationContext)
        val manager = CheckInManager(repository)

        // Check input data for FORCE flag (used by Test Button)
        val force = inputData.getBoolean("force", false)

        // Check if checked in today
        val isCheckedIn = manager.isCheckedInToday()
        if (isCheckedIn && !force) {
            return Result.success()
        }

        // Send Notification
        sendReminderNotification()
        return Result.success()
    }

    private fun sendReminderNotification() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Daily Check-in Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            // Note: Ensure you have a valid small icon. Using android.R.drawable.ic_dialog_info as fallback
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("今天还没报平安哦")
            .setContentText("是不是忘记打卡了？点我报个平安吧。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
