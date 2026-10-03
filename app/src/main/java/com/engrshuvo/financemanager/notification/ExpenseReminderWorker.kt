package com.engrshuvo.financemanager.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ExpenseReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepo = NotificationPreferencesRepository(applicationContext)
        val prefs = prefsRepo.preferencesFlow.first()

        // If user disabled reminders, do nothing
        if (!prefs.expenseReminderEnabled) {
            return Result.success()
        }

        // Check quiet hours
        if (prefs.quietHoursEnabled) {
            val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isInQuietHours = FinanceNotificationManager.isCurrentTimeInQuietHours(
                nowHour = nowHour,
                startHour = prefs.quietHoursStartHour,
                endHour = prefs.quietHoursEndHour
            )
            if (isInQuietHours) {
                // Silently skip notification during quiet period
                return Result.success()
            }
        }

        FinanceNotificationManager.showExpenseReminderNotification(
            context = applicationContext,
            title = "Expense reminder",
            body = "Have you recorded today's expenses? Open the app to update your records."
        )

        return Result.success()
    }
}
