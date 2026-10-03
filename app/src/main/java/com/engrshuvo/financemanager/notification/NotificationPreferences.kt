package com.engrshuvo.financemanager.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

data class NotificationPreferences(
    val morningNotificationEnabled: Boolean = true,
    val morningNotificationHour: Int = 8,
    val morningNotificationMinute: Int = 0,
    val expenseReminderEnabled: Boolean = true,
    val expenseReminderIntervalHours: Int = 4,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 8
)

class NotificationPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val MORNING_ENABLED = booleanPreferencesKey("morning_notification_enabled")
        val MORNING_HOUR = intPreferencesKey("morning_notification_hour")
        val MORNING_MINUTE = intPreferencesKey("morning_notification_minute")
        val REMINDER_ENABLED = booleanPreferencesKey("expense_reminder_enabled")
        val REMINDER_INTERVAL = intPreferencesKey("expense_reminder_interval_hours")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start_hour")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end_hour")
    }

    val preferencesFlow: Flow<NotificationPreferences> = context.notificationDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            NotificationPreferences(
                morningNotificationEnabled = preferences[PreferencesKeys.MORNING_ENABLED] ?: true,
                morningNotificationHour = preferences[PreferencesKeys.MORNING_HOUR] ?: 8,
                morningNotificationMinute = preferences[PreferencesKeys.MORNING_MINUTE] ?: 0,
                expenseReminderEnabled = preferences[PreferencesKeys.REMINDER_ENABLED] ?: true,
                expenseReminderIntervalHours = preferences[PreferencesKeys.REMINDER_INTERVAL] ?: 4,
                quietHoursEnabled = preferences[PreferencesKeys.QUIET_HOURS_ENABLED] ?: true,
                quietHoursStartHour = preferences[PreferencesKeys.QUIET_HOURS_START] ?: 22,
                quietHoursEndHour = preferences[PreferencesKeys.QUIET_HOURS_END] ?: 8
            )
        }

    suspend fun updateMorningNotificationEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.MORNING_ENABLED] = enabled
        }
    }

    suspend fun updateMorningNotificationTime(hour: Int, minute: Int) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.MORNING_HOUR] = hour.coerceIn(0, 23)
            preferences[PreferencesKeys.MORNING_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun updateExpenseReminderEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_ENABLED] = enabled
        }
    }

    suspend fun updateExpenseReminderInterval(hours: Int) {
        val validHours = if (hours in listOf(2, 3, 4, 6, 8, 12)) hours else 4
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_INTERVAL] = validHours
        }
    }

    suspend fun updateQuietHours(enabled: Boolean, startHour: Int, endHour: Int) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled
            preferences[PreferencesKeys.QUIET_HOURS_START] = startHour.coerceIn(0, 23)
            preferences[PreferencesKeys.QUIET_HOURS_END] = endHour.coerceIn(0, 23)
        }
    }
}
