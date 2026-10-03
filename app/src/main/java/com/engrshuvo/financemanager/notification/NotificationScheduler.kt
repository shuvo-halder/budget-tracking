package com.engrshuvo.financemanager.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    const val WORK_NAME_MORNING_BUDGET = "morning_daily_budget_work"
    const val WORK_NAME_EXPENSE_REMINDER = "periodic_expense_reminder_work"

    fun calculateInitialDelayMillis(
        targetHour: Int,
        targetMinute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
        }

        val targetCal = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
            set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCal.timeInMillis <= nowMillis) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return targetCal.timeInMillis - nowMillis
    }

    fun scheduleMorningBudgetWork(context: Context, prefs: NotificationPreferences) {
        val workManager = WorkManager.getInstance(context)

        if (!prefs.morningNotificationEnabled) {
            workManager.cancelUniqueWork(WORK_NAME_MORNING_BUDGET)
            return
        }

        val delayMillis = calculateInitialDelayMillis(
            targetHour = prefs.morningNotificationHour,
            targetMinute = prefs.morningNotificationMinute
        )

        val workRequest = OneTimeWorkRequestBuilder<MorningBudgetWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME_MORNING_BUDGET)
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME_MORNING_BUDGET,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun scheduleExpenseReminderWork(context: Context, prefs: NotificationPreferences) {
        val workManager = WorkManager.getInstance(context)

        if (!prefs.expenseReminderEnabled) {
            workManager.cancelUniqueWork(WORK_NAME_EXPENSE_REMINDER)
            return
        }

        val intervalHours = prefs.expenseReminderIntervalHours.toLong().coerceIn(2L, 24L)

        val workRequest = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(
            repeatInterval = intervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .addTag(WORK_NAME_EXPENSE_REMINDER)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_EXPENSE_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun syncAllWork(context: Context, prefs: NotificationPreferences) {
        scheduleMorningBudgetWork(context, prefs)
        scheduleExpenseReminderWork(context, prefs)
    }
}
