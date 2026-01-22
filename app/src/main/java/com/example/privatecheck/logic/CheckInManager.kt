package com.example.privatecheck.logic

import com.example.privatecheck.data.DataStoreRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import android.content.Context

class CheckInManager(private val repository: DataStoreRepository) {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE // YYYY-MM-DD

    suspend fun performCheckIn(context: Context) {
        val today = LocalDate.now()
        val todayStr = today.format(formatter)
        val timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val historyEntry = "$todayStr|$timeStr"
        
        val lastDateStr = repository.lastCheckInDate.first()
        val currentStreak = repository.streakDays.first()

        if (lastDateStr == todayStr) {
            // Ensure history is updated even if already checked in (in case feature was just added)
            repository.addCheckInDate(historyEntry)
            updateWidget(context)
            return
        }

        if (lastDateStr.isEmpty()) {
            // First time ever
            repository.addCheckInDate(historyEntry)
            repository.updateStreak(1)
            repository.updateLastCheckInDate(todayStr)
            updateWidget(context)
            return
        }

        val lastDate = LocalDate.parse(lastDateStr, formatter)
        
        // Calculate difference
        if (lastDate.plusDays(1).isEqual(today)) {
            repository.updateStreak(currentStreak + 1)
        } else {
            repository.updateStreak(1)
        }
        
        repository.addCheckInDate(historyEntry)
        repository.updateLastCheckInDate(todayStr)
        updateWidget(context)
    }

    private suspend fun updateWidget(context: Context) {
        try {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(com.example.privatecheck.widget.CheckInWidget::class.java)
            glanceIds.forEach { glanceId ->
                com.example.privatecheck.widget.CheckInWidget().update(context, glanceId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun isCheckedInToday(): Boolean {
        val todayStr = LocalDate.now().format(formatter)
        val lastDateStr = repository.lastCheckInDate.first()
        return todayStr == lastDateStr
    }
}
