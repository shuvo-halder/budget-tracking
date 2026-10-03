package com.engrshuvo.financemanager.notification

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
import com.engrshuvo.financemanager.MainActivity
import com.engrshuvo.financemanager.R

object FinanceNotificationManager {

    const val CHANNEL_ID_DAILY_BUDGET = "channel_daily_budget"
    const val CHANNEL_ID_EXPENSE_REMINDERS = "channel_expense_reminders"

    const val NOTIFICATION_ID_MORNING_BUDGET = 1001
    const val NOTIFICATION_ID_EXPENSE_REMINDER = 1002

    const val EXTRA_DESTINATION = "extra_destination"
    const val DESTINATION_DASHBOARD = "DASHBOARD"
    const val DESTINATION_ADD_EXPENSE = "ADD_EXPENSE"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val budgetChannel = NotificationChannel(
                CHANNEL_ID_DAILY_BUDGET,
                "Daily Budget Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning updates on your available daily budget and spending limits"
                enableLights(true)
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_EXPENSE_REMINDERS,
                "Expense Recording Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders to record your daily expenses"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(budgetChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun isCurrentTimeInQuietHours(nowHour: Int, startHour: Int, endHour: Int): Boolean {
        val normalizedNow = nowHour.coerceIn(0, 23)
        val normalizedStart = startHour.coerceIn(0, 23)
        val normalizedEnd = endHour.coerceIn(0, 23)

        return if (normalizedStart == normalizedEnd) {
            false // 0-length quiet hours
        } else if (normalizedStart < normalizedEnd) {
            normalizedNow in normalizedStart until normalizedEnd
        } else {
            // Wraps around midnight (e.g., 22:00 to 08:00)
            normalizedNow >= normalizedStart || normalizedNow < normalizedEnd
        }
    }

    fun showMorningBudgetNotification(
        context: Context,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_DASHBOARD)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MORNING_BUDGET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_DAILY_BUDGET)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING_BUDGET, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked concurrently
        }
    }

    fun showExpenseReminderNotification(
        context: Context,
        title: String = "Expense reminder",
        body: String = "Have you recorded today's expenses? Open the app to update your records."
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_ADD_EXPENSE)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_EXPENSE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_EXPENSE_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPENSE_REMINDER, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
