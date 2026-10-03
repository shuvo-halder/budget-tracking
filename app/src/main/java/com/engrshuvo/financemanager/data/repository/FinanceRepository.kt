package com.engrshuvo.financemanager.data.repository

import androidx.room.withTransaction
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.local.BudgetAllocationDao
import com.engrshuvo.financemanager.data.local.BudgetSettingDao
import com.engrshuvo.financemanager.data.local.LoanDao
import com.engrshuvo.financemanager.data.local.TransactionDao
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetSettingDao: BudgetSettingDao,
    private val loanDao: LoanDao,
    private val budgetAllocationDao: BudgetAllocationDao,
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

    /**
     * Soft-deletes (archives) a loan, cascading to its repayments and linked transaction.
     */
    suspend fun deleteLoan(
        loan: LoanEntity,
        archiveTimestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        archiveLoan(loan.id, archiveTimestamp)
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

    suspend fun recordLoanRepayment(
        loanId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        if (amount <= 0.0) {
            return@withContext false
        }
        runInTransaction {
            // Re-read fresh loan state inside the database transaction to prevent race conditions
            val loan = loanDao.getLoanByIdDirect(loanId) ?: return@runInTransaction false

            // Ineligible for repayment if settled, archived, or zero/negative remaining balance
            if (loan.status != LoanStatus.ACTIVE || loan.archivedAt != null || loan.remainingAmount <= 0.0) {
                return@runInTransaction false
            }

            // Overpayment rejected
            if (amount > loan.remainingAmount) {
                return@runInTransaction false
            }

            val newRemaining = (loan.remainingAmount - amount).coerceAtLeast(0.0)
            val newStatus = if (newRemaining <= 0.0) LoanStatus.SETTLED else LoanStatus.ACTIVE

            val updatedLoan = loan.copy(
                remainingAmount = newRemaining,
                status = newStatus
            )
            loanDao.updateLoan(updatedLoan)

            val repayment = LoanRepaymentEntity(
                id = 0,
                loanId = loanId,
                amount = amount,
                note = note,
                timestamp = timestamp
            )
            loanDao.insertRepayment(repayment)

            val repaymentType = if (loan.type == LoanType.LENT) TransactionType.INCOME else TransactionType.EXPENSE
            val catId = if (loan.type == LoanType.LENT) "loan_collected" else "loan_repaid"
            val catName = if (loan.type == LoanType.LENT) "Repayment from ${loan.personName}" else "Repaid to ${loan.personName}"

            transactionDao.insertTransaction(
                TransactionEntity(
                    id = 0,
                    type = repaymentType,
                    amount = amount,
                    categoryId = catId,
                    categoryName = catName,
                    note = note.ifBlank { "Loan repayment (${loan.personName})" },
                    timestamp = timestamp,
                    loanId = loanId
                )
            )
            true
        }
    }

    suspend fun markLoanSettled(loanId: Long): Boolean = withContext(Dispatchers.IO) {
        runInTransaction {
            val loan = loanDao.getLoanByIdDirect(loanId) ?: return@runInTransaction false
            if (loan.remainingAmount > 0 && loan.status == LoanStatus.ACTIVE && loan.archivedAt == null) {
                recordLoanRepayment(
                    loanId = loanId,
                    amount = loan.remainingAmount,
                    note = "Full Settlement"
                )
            } else if (loan.status != LoanStatus.SETTLED) {
                loanDao.updateLoan(loan.copy(status = LoanStatus.SETTLED))
                true
            } else {
                true
            }
        }
    }

    fun getRepaymentsForLoan(loanId: Long): Flow<List<LoanRepaymentEntity>> {
        return loanDao.getRepaymentsForLoanFlow(loanId).flowOn(Dispatchers.IO)
    }

    suspend fun setMonthlyBudgetLimit(limit: Double) = withContext(Dispatchers.IO) {
        budgetSettingDao.insertOrUpdateSetting(
            BudgetSettingEntity(
                settingKey = BudgetSettingEntity.KEY_MONTHLY_BUDGET,
                amountLimit = limit,
                currencyCode = "BDT"
            )
        )
    }

    // Daily Budget Limit stream strictly on Dispatchers.IO
    val dailyBudgetSetting: Flow<BudgetSettingEntity?> =
        budgetSettingDao.getSettingFlow(BudgetSettingEntity.KEY_DAILY_BUDGET).flowOn(Dispatchers.IO)

    val dailyBudgetLimit: Flow<Double> = dailyBudgetSetting.map { setting ->
        setting?.amountLimit ?: 500.0
    }.flowOn(Dispatchers.IO)

    suspend fun setDailyBudgetLimit(limit: Double) = withContext(Dispatchers.IO) {
        budgetSettingDao.insertOrUpdateSetting(
            BudgetSettingEntity(
                settingKey = BudgetSettingEntity.KEY_DAILY_BUDGET,
                amountLimit = limit,
                currencyCode = "BDT"
            )
        )
    }

    // Budget Allocations streams strictly on Dispatchers.IO
    val allBudgetAllocations: Flow<List<BudgetAllocationEntity>> =
        budgetAllocationDao.getAllAllocations().flowOn(Dispatchers.IO)

    fun getAllocationsForMonth(monthKey: String): Flow<List<BudgetAllocationEntity>> {
        return budgetAllocationDao.getAllocationsForMonth(monthKey).flowOn(Dispatchers.IO)
    }

    suspend fun getAllocationsForMonthDirect(monthKey: String): List<BudgetAllocationEntity> =
        withContext(Dispatchers.IO) {
            budgetAllocationDao.getAllocationsForMonthDirect(monthKey)
        }

    suspend fun saveBudgetAllocation(allocation: BudgetAllocationEntity) = withContext(Dispatchers.IO) {
        budgetAllocationDao.insertOrUpdateAllocation(allocation)
    }

    suspend fun saveBudgetAllocations(allocations: List<BudgetAllocationEntity>) = withContext(Dispatchers.IO) {
        budgetAllocationDao.insertOrUpdateAllocations(allocations)
    }

    suspend fun deleteBudgetAllocation(monthKey: String, categoryId: String) = withContext(Dispatchers.IO) {
        budgetAllocationDao.deleteAllocation(monthKey, categoryId)
    }

    /**
     * Copies budget allocation targets from one month to another month.
     * CRITICAL: Strictly copies budget allocation plans only!
     * NEVER copies transactions, expenses, income, savings transfers, or loans.
     */
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

    /**
     * Calculates the expiration cutoff timestamp exactly 2 calendar months before [currentTimeMillis]
     * using calendar-month arithmetic.
     */
    fun getTwoMonthsExpirationCutoff(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            add(Calendar.MONTH, -2)
        }
        return cal.timeInMillis
    }

    /**
     * Permanently deletes archived records whose retention period (2 calendar months) has expired.
     * Idempotent and safe to run on startup, on resume, and on demand.
     */
    suspend fun purgeExpiredArchivedRecords(
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Int = withContext(Dispatchers.IO) {
        val cutoff = getTwoMonthsExpirationCutoff(currentTimeMillis)
        val purgedTransactions = transactionDao.purgeExpiredTransactions(cutoff)
        val purgedRepayments = loanDao.purgeExpiredRepayments(cutoff)
        val purgedLoans = loanDao.purgeExpiredLoans(cutoff)
        purgedTransactions + purgedRepayments + purgedLoans
    }
}
