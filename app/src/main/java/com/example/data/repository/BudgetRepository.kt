package com.example.data.repository

import com.example.data.local.BudgetSettingDao
import com.example.data.local.TransactionDao
import com.example.data.model.BudgetSettingEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository(
    private val transactionDao: TransactionDao,
    private val budgetSettingDao: BudgetSettingDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    val totalIncome: Flow<Double> = transactionDao.getTotalIncomeFlow()

    val totalExpense: Flow<Double> = transactionDao.getTotalExpenseFlow()

    fun getMonthlyExpense(startOfMonth: Long): Flow<Double> =
        transactionDao.getMonthlyExpenseFlow(startOfMonth)

    val monthlyBudgetSetting: Flow<BudgetSettingEntity?> =
        budgetSettingDao.getSettingFlow(BudgetSettingEntity.KEY_MONTHLY_BUDGET)

    val monthlyBudgetLimit: Flow<Double> = monthlyBudgetSetting.map { setting ->
        setting?.amountLimit ?: 30000.0
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun setMonthlyBudgetLimit(limit: Double) {
        budgetSettingDao.insertOrUpdateSetting(
            BudgetSettingEntity(
                settingKey = BudgetSettingEntity.KEY_MONTHLY_BUDGET,
                amountLimit = limit,
                currencyCode = "BDT"
            )
        )
    }
}
