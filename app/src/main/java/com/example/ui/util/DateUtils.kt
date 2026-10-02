package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val headerDateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())

    fun formatTransactionTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val transDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        val timeStr = timeFormat.format(Date(timestamp))

        return when {
            isSameDay(now, transDate) -> "Today • $timeStr"
            isYesterday(now, transDate) -> "Yesterday • $timeStr"
            else -> "${fullDateFormat.format(Date(timestamp))} • $timeStr"
        }
    }

    fun formatHeaderDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val transDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, transDate) -> "Today"
            isYesterday(now, transDate) -> "Yesterday"
            else -> fullDateFormat.format(Date(timestamp))
        }
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getCurrentMonthName(): String {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return monthFormat.format(cal.time)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        val yesterday = (cal1.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, cal2)
    }
}
