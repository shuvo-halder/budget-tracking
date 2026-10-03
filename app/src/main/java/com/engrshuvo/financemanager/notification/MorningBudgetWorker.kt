package com.engrshuvo.financemanager.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import kotlinx.coroutines.flow.first

class MorningBudgetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepo = NotificationPreferencesRepository(applicationContext)
        val prefs = prefsRepo.preferencesFlow.first()

        // If user disabled morning notifications, do not notify
        if (!prefs.morningNotificationEnabled) {
            return Result.success()
        }

        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dailySetting = db.budgetSettingDao().getSettingDirect(BudgetSettingEntity.KEY_DAILY_BUDGET)
            val dailyLimit = dailySetting?.amountLimit ?: 0.0

            val startOfToday = DateUtils.getStartOfDay()
            val endOfToday = DateUtils.getEndOfDay()
            val todayTransactions = db.transactionDao().getTransactionsInDateRangeDirect(startOfToday, endOfToday)

            // Strictly filter ordinary living expenses (excluding loan repayments to keep operating calculation clean)
            val todayExpenses = todayTransactions
                .filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" && it.archivedAt == null }
                .sumOf { it.amount }

            val title: String
            val body: String

            if (dailyLimit > 0.0) {
                val dailyRemaining = dailyLimit - todayExpenses
                title = "Good morning! Your daily budget"
                body = if (todayExpenses > 0.0) {
                    "You have ${CurrencyUtils.formatBDT(dailyRemaining)} remaining for today (Spent: ${CurrencyUtils.formatBDT(todayExpenses)} of ${CurrencyUtils.formatBDT(dailyLimit)})."
                } else {
                    "You have ${CurrencyUtils.formatBDT(dailyRemaining)} available for today's planned spending."
                }
            } else {
                title = "Good morning! Daily budget setup"
                body = "No daily budget set. Open the app to set today's spending limit."
            }

            FinanceNotificationManager.showMorningBudgetNotification(
                context = applicationContext,
                title = title,
                body = body
            )
        } catch (e: Exception) {
            // Safe fallback to avoid crashes
        } finally {
            // Automatically schedule next morning's run to maintain local time accuracy
            NotificationScheduler.scheduleMorningBudgetWork(applicationContext, prefs)
        }

        return Result.success()
    }
}
