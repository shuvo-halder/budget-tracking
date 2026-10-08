package com.engrshuvo.financemanager.data.repository

import androidx.room.withTransaction
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.local.BudgetAllocationDao
import com.engrshuvo.financemanager.data.local.BudgetSettingDao
import com.engrshuvo.financemanager.data.local.FinancialGoalDao
import com.engrshuvo.financemanager.data.local.GoalContributionDao
import com.engrshuvo.financemanager.data.local.LoanDao
import com.engrshuvo.financemanager.data.local.TransactionDao
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.FinanceBackupData
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetSettingDao: BudgetSettingDao,
    private val loanDao: LoanDao,
    private val budgetAllocationDao: BudgetAllocationDao,
    private val financialGoalDao: FinancialGoalDao? = null,
    private val goalContributionDao: GoalContributionDao? = null,
    private val database: AppDatabase? = null
) {
    // Transaction streams strictly on Dispatchers.IO
    val allTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions().flowOn(Dispatchers.IO)

    val totalIncome: Flow<Double> =
        transactionDao.getTotalIncomeFlow().flowOn(Dispatchers.IO)

    val totalExpense: Flow<Double> =
        transactionDao.getTotalExpenseFlow().flowOn(Dispatchers.IO)

    // Loan streams strictly on Dispatchers.IO
    val allLoans: Flow<List<LoanEntity>> =
        loanDao.getAllLoansFlow().flowOn(Dispatchers.IO)

    val activeLoans: Flow<List<LoanEntity>> =
        loanDao.getActiveLoansFlow().flowOn(Dispatchers.IO)

    val totalActiveLent: Flow<Double> =
        loanDao.getTotalActiveLentFlow().flowOn(Dispatchers.IO)

    val totalActiveBorrowed: Flow<Double> =
        loanDao.getTotalActiveBorrowedFlow().flowOn(Dispatchers.IO)

    // Budget Limit stream strictly on Dispatchers.IO
    val monthlyBudgetSetting: Flow<BudgetSettingEntity?> =
        budgetSettingDao.getSettingFlow(BudgetSettingEntity.KEY_MONTHLY_BUDGET).flowOn(Dispatchers.IO)

    val monthlyBudgetLimit: Flow<Double> = monthlyBudgetSetting.map { setting ->
        setting?.amountLimit ?: 30000.0
    }.flowOn(Dispatchers.IO)

    // Archive streams strictly on Dispatchers.IO
    val archivedTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getArchivedTransactions().flowOn(Dispatchers.IO)

    val archivedLoans: Flow<List<LoanEntity>> =
        loanDao.getArchivedLoans().flowOn(Dispatchers.IO)

    // Financial Goals & Contributions streams strictly on Dispatchers.IO
    val allGoals: Flow<List<FinancialGoalEntity>> =
        (financialGoalDao?.getAllGoalsFlow() ?: flowOf(emptyList())).flowOn(Dispatchers.IO)

    val activeGoals: Flow<List<FinancialGoalEntity>> =
        (financialGoalDao?.getActiveGoalsFlow() ?: flowOf(emptyList())).flowOn(Dispatchers.IO)

    val archivedGoals: Flow<List<FinancialGoalEntity>> =
        (financialGoalDao?.getArchivedGoalsFlow() ?: flowOf(emptyList())).flowOn(Dispatchers.IO)

    val allGoalContributions: Flow<List<GoalContributionEntity>> =
        (goalContributionDao?.getAllContributionsFlow() ?: flowOf(emptyList())).flowOn(Dispatchers.IO)

    val archivedGoalContributions: Flow<List<GoalContributionEntity>> =
        (goalContributionDao?.getArchivedContributionsFlow() ?: flowOf(emptyList())).flowOn(Dispatchers.IO)

    fun getContributionsForGoal(goalId: Long): Flow<List<GoalContributionEntity>> =
        (goalContributionDao?.getContributionsForGoalFlow(goalId) ?: flowOf(emptyList())).flowOn(Dispatchers.IO)

    fun getGoalById(id: Long): Flow<FinancialGoalEntity?> =
        (financialGoalDao?.getGoalByIdFlow(id) ?: flowOf(null)).flowOn(Dispatchers.IO)

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    /**
     * Soft-deletes (archives) a transaction and any linked loan.
     */
    suspend fun deleteTransaction(
        transaction: TransactionEntity,
        archiveTimestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        archiveTransaction(transaction.id, archiveTimestamp)
        if (transaction.loanId != null) {
            archiveLoan(transaction.loanId, archiveTimestamp)
        }
    }

    suspend fun archiveTransaction(
        id: Long,
        archiveTimestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        transactionDao.archiveTransaction(id, archiveTimestamp)
    }

    suspend fun restoreTransaction(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.restoreTransaction(id)
        val transaction = transactionDao.getTransactionByIdDirect(id)
        if (transaction?.loanId != null) {
            restoreLoan(transaction.loanId)
        }
    }

    suspend fun permanentlyDeleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.permanentlyDeleteTransactionById(id)
    }

    private suspend fun <R> runInTransaction(block: suspend () -> R): R {
        val db = database
        return if (db != null) {
            db.withTransaction {
                block()
            }
        } else {
            block()
        }
    }

    suspend fun insertLoanWithTransaction(
        loan: LoanEntity,
        createLinkedTransaction: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        runInTransaction {
            val loanId = loanDao.insertLoan(loan)
            if (createLinkedTransaction) {
                val transType = TransactionType.LOAN
                val categoryId = if (loan.type == LoanType.LENT) "loan_lent" else "loan_borrowed"
                val categoryName = if (loan.type == LoanType.LENT) "Loan to ${loan.personName}" else "Loan from ${loan.personName}"
                val transEntity = TransactionEntity(
                    id = 0,
                    type = transType,
                    amount = loan.initialAmount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    note = loan.note.ifBlank { if (loan.type == LoanType.LENT) "Lent to ${loan.personName}" else "Borrowed from ${loan.personName}" },
                    timestamp = loan.startDate,
                    loanId = loanId
                )
                transactionDao.insertTransaction(transEntity)
            }
            loanId
        }
    }

    suspend fun updateLoan(loan: LoanEntity) = withContext(Dispatchers.IO) {
        loanDao.updateLoan(loan)
    }

    suspend fun archiveLoan(
        loanId: Long,
        archiveTimestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        runInTransaction {
            loanDao.archiveLoan(loanId, archiveTimestamp)
            loanDao.archiveRepaymentsForLoan(loanId, archiveTimestamp)
            transactionDao.archiveTransactionsForLoan(loanId, archiveTimestamp)
        }
    }

    suspend fun restoreLoan(loanId: Long) = withContext(Dispatchers.IO) {
        runInTransaction {
            loanDao.restoreLoan(loanId)
            loanDao.restoreRepaymentsForLoan(loanId)
            transactionDao.restoreTransactionsForLoan(loanId)
        }
    }

    suspend fun permanentlyDeleteLoan(loanId: Long) = withContext(Dispatchers.IO) {
        runInTransaction {
            transactionDao.permanentlyDeleteTransactionsForLoan(loanId)
            loanDao.permanentlyDeleteRepaymentsForLoan(loanId)
            loanDao.permanentlyDeleteLoanById(loanId)
        }
    }

    suspend fun deleteLoan(loan: LoanEntity) = withContext(Dispatchers.IO) {
        archiveLoan(loan.id)
    }

    suspend fun addLoanRepayment(repayment: LoanRepaymentEntity): Long = withContext(Dispatchers.IO) {
        runInTransaction {
            val repId = loanDao.insertRepayment(repayment)
            val currentLoan = loanDao.getLoanByIdDirect(repayment.loanId)
            if (currentLoan != null) {
                val updatedRemaining = (currentLoan.remainingAmount - repayment.amount).coerceAtLeast(0.0)
                val updatedStatus = if (updatedRemaining == 0.0) LoanStatus.SETTLED else currentLoan.status
                loanDao.updateLoan(
                    currentLoan.copy(
                        remainingAmount = updatedRemaining,
                        status = updatedStatus
                    )
                )

                val transType = TransactionType.LOAN
                val catId = if (currentLoan.type == LoanType.LENT) "loan_collected" else "loan_repaid"
                val catName = if (currentLoan.type == LoanType.LENT) "Collection from ${currentLoan.personName}" else "Repayment to ${currentLoan.personName}"
                transactionDao.insertTransaction(
                    TransactionEntity(
                        id = 0,
                        type = transType,
                        amount = repayment.amount,
                        categoryId = catId,
                        categoryName = catName,
                        note = repayment.note.ifBlank { "Loan repayment #${repId}" },
                        timestamp = repayment.timestamp,
                        loanId = currentLoan.id
                    )
                )
            }
            repId
        }
    }

    fun getRepaymentsForLoan(loanId: Long): Flow<List<LoanRepaymentEntity>> =
        loanDao.getRepaymentsForLoanFlow(loanId).flowOn(Dispatchers.IO)

    suspend fun recordLoanRepayment(loanId: Long, amount: Double, note: String): Boolean = withContext(Dispatchers.IO) {
        val currentLoan = loanDao.getLoanByIdDirect(loanId) ?: return@withContext false
        if (amount <= 0.0 || amount > currentLoan.remainingAmount) {
            return@withContext false
        }
        val repayment = LoanRepaymentEntity(
            loanId = loanId,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            note = note
        )
        val repId = addLoanRepayment(repayment)
        repId > 0L
    }

    suspend fun markLoanSettled(loanId: Long): Boolean = withContext(Dispatchers.IO) {
        val currentLoan = loanDao.getLoanByIdDirect(loanId) ?: return@withContext false
        loanDao.updateLoan(
            currentLoan.copy(
                remainingAmount = 0.0,
                status = LoanStatus.SETTLED
            )
        )
        true
    }

    // Financial Goals repository methods
    suspend fun insertGoal(goal: FinancialGoalEntity): Long = withContext(Dispatchers.IO) {
        financialGoalDao?.insertGoal(goal) ?: 0L
    }

    suspend fun updateGoal(goal: FinancialGoalEntity) = withContext(Dispatchers.IO) {
        financialGoalDao?.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun archiveGoal(
        goalId: Long,
        archiveTimestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        runInTransaction {
            financialGoalDao?.archiveGoal(goalId, archiveTimestamp)
            goalContributionDao?.archiveContributionsForGoal(goalId, archiveTimestamp)
        }
    }

    suspend fun restoreGoal(goalId: Long) = withContext(Dispatchers.IO) {
        runInTransaction {
            financialGoalDao?.restoreGoal(goalId)
            goalContributionDao?.restoreContributionsForGoal(goalId)
        }
    }

    suspend fun permanentlyDeleteGoal(goalId: Long) = withContext(Dispatchers.IO) {
        runInTransaction {
            goalContributionDao?.permanentlyDeleteContributionsForGoal(goalId)
            financialGoalDao?.permanentlyDeleteGoalById(goalId)
        }
    }

    suspend fun addGoalContribution(contribution: GoalContributionEntity): Long = withContext(Dispatchers.IO) {
        runInTransaction {
            val contribId = goalContributionDao?.insertContribution(contribution) ?: 0L
            val goal = financialGoalDao?.getGoalByIdDirect(contribution.goalId)
            if (goal != null) {
                financialGoalDao.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
            }
            contribId
        }
    }

    suspend fun updateGoalContribution(contribution: GoalContributionEntity) = withContext(Dispatchers.IO) {
        goalContributionDao?.updateContribution(contribution)
    }

    suspend fun deleteGoalContribution(contributionId: Long) = withContext(Dispatchers.IO) {
        goalContributionDao?.deleteContribution(contributionId)
    }

    suspend fun archiveGoalContribution(
        id: Long,
        archiveTimestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        goalContributionDao?.archiveContribution(id, archiveTimestamp)
    }

    suspend fun restoreGoalContribution(id: Long) = withContext(Dispatchers.IO) {
        goalContributionDao?.restoreContribution(id)
    }

    suspend fun getGoalByIdDirect(id: Long): FinancialGoalEntity? = withContext(Dispatchers.IO) {
        financialGoalDao?.getGoalByIdDirect(id)
    }

    // Budget Limits
    suspend fun updateMonthlyBudgetLimit(limit: Double) = withContext(Dispatchers.IO) {
        budgetSettingDao.insertOrUpdateSetting(
            BudgetSettingEntity(
                settingKey = BudgetSettingEntity.KEY_MONTHLY_BUDGET,
                amountLimit = limit
            )
        )
    }

    suspend fun setMonthlyBudgetLimit(limit: Double) = updateMonthlyBudgetLimit(limit)

    val dailyBudgetSetting: Flow<BudgetSettingEntity?> =
        budgetSettingDao.getSettingFlow(BudgetSettingEntity.KEY_DAILY_BUDGET).flowOn(Dispatchers.IO)

    val dailyBudgetLimit: Flow<Double> = dailyBudgetSetting.map { setting ->
        setting?.amountLimit ?: 500.0
    }.flowOn(Dispatchers.IO)

    suspend fun updateDailyBudgetLimit(limit: Double) = withContext(Dispatchers.IO) {
        budgetSettingDao.insertOrUpdateSetting(
            BudgetSettingEntity(
                settingKey = BudgetSettingEntity.KEY_DAILY_BUDGET,
                amountLimit = limit
            )
        )
    }

    suspend fun setDailyBudgetLimit(limit: Double) = updateDailyBudgetLimit(limit)

    // Budget Allocations
    fun getAllocationsForMonth(monthKey: String): Flow<List<BudgetAllocationEntity>> =
        budgetAllocationDao.getAllocationsForMonth(monthKey).flowOn(Dispatchers.IO)

    val allBudgetAllocations: Flow<List<BudgetAllocationEntity>> =
        budgetAllocationDao.getAllAllocations().flowOn(Dispatchers.IO)

    suspend fun setBudgetAllocation(allocation: BudgetAllocationEntity) = withContext(Dispatchers.IO) {
        budgetAllocationDao.insertOrUpdateAllocation(allocation)
    }

    suspend fun setBudgetAllocations(allocations: List<BudgetAllocationEntity>) = withContext(Dispatchers.IO) {
        budgetAllocationDao.insertOrUpdateAllocations(allocations)
    }

    suspend fun saveBudgetAllocations(allocations: List<BudgetAllocationEntity>) = setBudgetAllocations(allocations)

    suspend fun deleteBudgetAllocation(monthKey: String, categoryId: String) = withContext(Dispatchers.IO) {
        budgetAllocationDao.deleteAllocation(monthKey, categoryId)
    }

    suspend fun copyAllocations(fromMonthKey: String, toMonthKey: String) = withContext(Dispatchers.IO) {
        val previousAllocations = budgetAllocationDao.getAllocationsForMonthDirect(fromMonthKey)
        if (previousAllocations.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val newAllocations = previousAllocations.map {
                it.copy(monthKey = toMonthKey, updatedAt = now)
            }
            budgetAllocationDao.insertOrUpdateAllocations(newAllocations)
        }
    }

    fun getTwoMonthsExpirationCutoff(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            add(Calendar.MONTH, -2)
        }
        return cal.timeInMillis
    }

    suspend fun purgeExpiredArchivedRecords(
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Int = withContext(Dispatchers.IO) {
        val cutoff = getTwoMonthsExpirationCutoff(currentTimeMillis)
        val purgedTransactions = transactionDao.purgeExpiredTransactions(cutoff)
        val purgedRepayments = loanDao.purgeExpiredRepayments(cutoff)
        val purgedLoans = loanDao.purgeExpiredLoans(cutoff)
        val purgedGoals = financialGoalDao?.purgeExpiredGoals(cutoff) ?: 0
        val purgedContributions = goalContributionDao?.purgeExpiredContributions(cutoff) ?: 0
        purgedTransactions + purgedRepayments + purgedLoans + purgedGoals + purgedContributions
    }

    suspend fun exportBackupData(): FinanceBackupData = withContext(Dispatchers.IO) {
        val txs = transactionDao.getAllTransactionsForBackup()
        val loans = loanDao.getAllLoansForBackup()
        val repayments = loanDao.getAllRepaymentsForBackup()
        val settings = budgetSettingDao.getAllSettingsForBackup()
        val allocations = budgetAllocationDao.getAllAllocationsDirect()
        val goals = financialGoalDao?.getAllGoalsForBackup() ?: emptyList()
        val contributions = goalContributionDao?.getAllContributionsForBackup() ?: emptyList()

        FinanceBackupData(
            exportTimestamp = System.currentTimeMillis(),
            transactions = txs,
            loans = loans,
            loanRepayments = repayments,
            budgetSettings = settings,
            budgetAllocations = allocations,
            financialGoals = goals,
            goalContributions = contributions
        )
    }

    suspend fun restoreBackupData(backupData: FinanceBackupData): Result<Int> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                // 1. Clear tables in child-to-parent order
                goalContributionDao?.clearAllContributions()
                financialGoalDao?.clearAllGoals()
                loanDao.clearAllRepayments()
                transactionDao.clearAllTransactions()
                loanDao.clearAllLoans()
                budgetAllocationDao.clearAllAllocations()
                budgetSettingDao.clearAllSettings()

                // 2. Insert parent tables first
                if (backupData.loans.isNotEmpty()) {
                    loanDao.insertAllLoans(backupData.loans)
                }
                if (backupData.loanRepayments.isNotEmpty()) {
                    loanDao.insertAllRepayments(backupData.loanRepayments)
                }
                if (backupData.transactions.isNotEmpty()) {
                    transactionDao.insertAllTransactions(backupData.transactions)
                }
                if (backupData.budgetSettings.isNotEmpty()) {
                    budgetSettingDao.insertAllSettings(backupData.budgetSettings)
                }
                if (backupData.budgetAllocations.isNotEmpty()) {
                    budgetAllocationDao.insertOrUpdateAllocations(backupData.budgetAllocations)
                }
                if (backupData.financialGoals.isNotEmpty()) {
                    financialGoalDao?.insertAllGoals(backupData.financialGoals)
                }
                if (backupData.goalContributions.isNotEmpty()) {
                    goalContributionDao?.insertAllContributions(backupData.goalContributions)
                }
            }
            val totalRestored = backupData.transactions.size + backupData.loans.size +
                    backupData.loanRepayments.size + backupData.budgetAllocations.size +
                    backupData.financialGoals.size + backupData.goalContributions.size
            Result.success(totalRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
