package com.example.privatecheck.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.privatecheck.data.DataStoreRepository
import com.example.privatecheck.logic.EmailService
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CheckPenaltyWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = DataStoreRepository(context)

    override suspend fun doWork(): Result {
        Log.d("CheckPenaltyWorker", "Checking for missed check-ins...")

        val lastDateStr = repository.lastCheckInDate.first()
        if (lastDateStr.isEmpty()) {
            return Result.success() // No data yet
        }

        val today = LocalDate.now()
        val lastDate = LocalDate.parse(lastDateStr)

        val diffDays = ChronoUnit.DAYS.between(lastDate, today)
        // logic: 
        // last checkin: Jan 1.
        // Jan 2 missed.
        // Jan 3 missed. 
        // Jan 4 (diff=3). -> 2 full days missed.
        
        if (diffDays >= 3) {
            // Check if already sent today (to avoid retry loops or double sending in same day)
            // Ideally we also track if we sent for this incident, but sending daily reminder is good for penalty.
            
            // For MVP, simplistic check:
             val contacts = listOf(
                 repository.contactEmail.first(),
                 repository.contactEmail2.first(),
                 repository.contactEmail3.first()
             ).map { it.trim() }.filter { it.isNotBlank() }.distinct()

             val sender = repository.senderEmail.first()
             val pass = repository.senderPassword.first()
             val host = repository.smtpHost.first()
             val streak = repository.streakDays.first()

             if (contacts.isNotEmpty() && sender.isNotBlank()) {
                 var anySuccess = false
                 contacts.forEach { contact ->
                     val success = EmailService.sendEmail(
                         toEmail = contact,
                         subject = "【私了吗】您的好友中断了打卡！",
                         body = "紧急通知：\n\n您的好友在《私了吗》App 中已经连续 ${diffDays - 1} 天未打卡了（上次打卡：$lastDateStr）。\n\n之前的连胜纪录：$streak 天。\n\n请督促他/她重新开始！",
                         senderEmail = sender,
                         senderPassword = pass,
                         smtpHost = host
                     )
                     if (success) anySuccess = true
                 }

                 if (anySuccess) {
                     Log.d("CheckPenaltyWorker", "Penalty email(s) sent.")
                 } else {
                     Log.e("CheckPenaltyWorker", "Failed to send any penalty emails.")
                     return Result.retry()
                 }
             }
        }

        return Result.success()
    }
}
