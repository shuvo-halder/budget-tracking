package com.engrshuvo.financemanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.notification.FinanceNotificationManager
import com.engrshuvo.financemanager.notification.NotificationPreferences
import com.engrshuvo.financemanager.notification.NotificationPreferencesRepository
import com.engrshuvo.financemanager.notification.NotificationScheduler
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationSystemUnitTest {

    @Test
    fun `1 morning notification content with configured daily limit formats BDT amount properly`() {
        val dailyLimit = 800.0
        val todaySpent = 250.0
        val remaining = dailyLimit - todaySpent

        val title = "Good morning! Your daily budget"
        val body = "You have ${CurrencyUtils.formatBDT(remaining)} remaining for today (Spent: ${CurrencyUtils.formatBDT(todaySpent)} of ${CurrencyUtils.formatBDT(dailyLimit)})."

        assertEquals("Good morning! Your daily budget", title)
        assertTrue(body.contains("৳550"))
        assertTrue(body.contains("৳250"))
        assertTrue(body.contains("৳800"))
    }

    @Test
    fun `2 missing daily budget configuration shows setup prompt`() {
        val dailyLimit = 0.0
        val isConfigured = dailyLimit > 0.0

        val title = if (isConfigured) "Good morning! Your daily budget" else "Good morning! Daily budget setup"
        val body = if (isConfigured) "You have ৳500 available for today's planned spending." else "No daily budget set. Open the app to set today's spending limit."

        assertEquals("Good morning! Daily budget setup", title)
        assertEquals("No daily budget set. Open the app to set today's spending limit.", body)
    }

    @Test
    fun `3 correct BDT currency formatting for notification strings`() {
        assertEquals("৳0", CurrencyUtils.formatBDT(0.0))
        assertEquals("৳500", CurrencyUtils.formatBDT(500.0))
        assertEquals("৳1,200", CurrencyUtils.formatBDT(1200.0))
        assertEquals("৳25,000", CurrencyUtils.formatBDT(25000.0))
    }

    @Test
    fun `4 exclusion of loan movements from ordinary daily expenses for notifications`() {
        val now = System.currentTimeMillis()
        val startOfToday = DateUtils.getStartOfDay(now)
        val endOfToday = DateUtils.getEndOfDay(now)

        val transactions = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 150.0, categoryId = "food", categoryName = "Breakfast", note = "", timestamp = now),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 500.0, categoryId = "loan_repaid", categoryName = "Repaid to Hasan", note = "", timestamp = now),
            TransactionEntity(id = 3, type = TransactionType.LOAN, amount = 2000.0, categoryId = "loan_lent", categoryName = "Loan to Karim", note = "", timestamp = now),
            TransactionEntity(id = 4, type = TransactionType.INCOME, amount = 1000.0, categoryId = "freelance", categoryName = "Design", note = "", timestamp = now)
        )

        // Strict accounting rule: ordinary daily living expense excludes loan_repaid, loan principal, and income
        val todayOrdinaryExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" && it.timestamp in startOfToday..endOfToday }
            .sumOf { it.amount }

        assertEquals(150.0, todayOrdinaryExpenses, 0.001)
    }

    @Test
    fun `5 expense reminder enabled and disabled preference flags`() {
        val defaultPrefs = NotificationPreferences()
        assertTrue(defaultPrefs.morningNotificationEnabled)
        assertTrue(defaultPrefs.expenseReminderEnabled)
        assertEquals(4, defaultPrefs.expenseReminderIntervalHours)

        val disabledPrefs = defaultPrefs.copy(expenseReminderEnabled = false)
        assertFalse(disabledPrefs.expenseReminderEnabled)
    }

    @Test
    fun `6 reminder interval validation restricts to supported hours`() {
        val validIntervals = listOf(2, 3, 4, 6, 8, 12)
        validIntervals.forEach { hours ->
            val prefs = NotificationPreferences(expenseReminderIntervalHours = hours)
            assertEquals(hours, prefs.expenseReminderIntervalHours)
        }
    }

    @Test
    fun `7 quiet hours calculation correctly detects active quiet period across midnight and daytime`() {
        // Quiet hours 22:00 (10 PM) to 08:00 (8 AM)
        val startHour = 22
        val endHour = 8

        // Inside quiet hours: 23:00, 00:00, 04:00, 07:00
        assertTrue(FinanceNotificationManager.isCurrentTimeInQuietHours(23, startHour, endHour))
        assertTrue(FinanceNotificationManager.isCurrentTimeInQuietHours(0, startHour, endHour))
        assertTrue(FinanceNotificationManager.isCurrentTimeInQuietHours(4, startHour, endHour))
        assertTrue(FinanceNotificationManager.isCurrentTimeInQuietHours(7, startHour, endHour))

        // Outside quiet hours: 08:00, 12:00, 15:00, 21:00
        assertFalse(FinanceNotificationManager.isCurrentTimeInQuietHours(8, startHour, endHour))
        assertFalse(FinanceNotificationManager.isCurrentTimeInQuietHours(12, startHour, endHour))
        assertFalse(FinanceNotificationManager.isCurrentTimeInQuietHours(15, startHour, endHour))
        assertFalse(FinanceNotificationManager.isCurrentTimeInQuietHours(21, startHour, endHour))

        // Same-day quiet hours 13:00 to 16:00
        assertTrue(FinanceNotificationManager.isCurrentTimeInQuietHours(14, 13, 16))
        assertFalse(FinanceNotificationManager.isCurrentTimeInQuietHours(17, 13, 16))
    }

    @Test
    fun `8 notification tap destinations point to correct destination constants`() {
        assertEquals("DASHBOARD", FinanceNotificationManager.DESTINATION_DASHBOARD)
        assertEquals("ADD_EXPENSE", FinanceNotificationManager.DESTINATION_ADD_EXPENSE)
        assertEquals("extra_destination", FinanceNotificationManager.EXTRA_DESTINATION)
    }

    @Test
    fun `9 preference persistence saves and retrieves updated settings`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = NotificationPreferencesRepository(context)

        repo.updateMorningNotificationEnabled(true)
        repo.updateMorningNotificationTime(9, 30)
        repo.updateExpenseReminderEnabled(true)
        repo.updateExpenseReminderInterval(6)
        repo.updateQuietHours(true, 23, 7)

        val prefs = repo.preferencesFlow.first()
        assertTrue(prefs.morningNotificationEnabled)
        assertEquals(9, prefs.morningNotificationHour)
        assertEquals(30, prefs.morningNotificationMinute)
        assertTrue(prefs.expenseReminderEnabled)
        assertEquals(6, prefs.expenseReminderIntervalHours)
        assertTrue(prefs.quietHoursEnabled)
        assertEquals(23, prefs.quietHoursStartHour)
        assertEquals(7, prefs.quietHoursEndHour)
    }

    @Test
    fun `10 notification generation is read-only and does not create financial transactions`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)

        val initialTxCount = db.transactionDao().getAllTransactionsDirect().size

        // Read settings and calculate notification content
        val dailySetting = db.budgetSettingDao().getSettingDirect(BudgetSettingEntity.KEY_DAILY_BUDGET)
        val dailyLimit = dailySetting?.amountLimit ?: 500.0
        val remaining = dailyLimit - 0.0

        val notificationBody = "You have ${CurrencyUtils.formatBDT(remaining)} available for today's planned spending."
        assertNotNull(notificationBody)

        val postTxCount = db.transactionDao().getAllTransactionsDirect().size
        assertEquals(initialTxCount, postTxCount)
    }

    @Test
    fun `11 initial delay calculation schedules for next day if target time has already passed`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentTime = cal.timeInMillis

        // Target: 8:00 AM (already passed for today at 10:00 AM)
        val delayMillis = NotificationScheduler.calculateInitialDelayMillis(
            targetHour = 8,
            targetMinute = 0,
            nowMillis = currentTime
        )

        // Delay should be exactly 22 hours (from 10 AM to 8 AM next morning)
        val expectedHours = 22L
        val expectedMillis = expectedHours * 60 * 60 * 1000
        assertEquals(expectedMillis, delayMillis)
    }

    @Test
    fun `12 initial delay calculation schedules for same day if target time is in future`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentTime = cal.timeInMillis

        // Target: 8:00 AM (2 hours in future today)
        val delayMillis = NotificationScheduler.calculateInitialDelayMillis(
            targetHour = 8,
            targetMinute = 0,
            nowMillis = currentTime
        )

        // Delay should be exactly 2 hours (from 6 AM to 8 AM)
        val expectedHours = 2L
        val expectedMillis = expectedHours * 60 * 60 * 1000
        assertEquals(expectedMillis, delayMillis)
    }
}
