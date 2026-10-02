package com.engrshuvo.financemanager.data.repository

import com.engrshuvo.financemanager.data.local.BudgetSettingDao
import com.engrshuvo.financemanager.data.local.LoanDao
import com.engrshuvo.financemanager.data.local.TransactionDao
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

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetSettingDao: BudgetSettingDao,
    private val loanDao: LoanDao
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

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
        if (transaction.loanId != null) {
            loanDao.deleteLoanById(transaction.loanId)
        }
    }

    suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun insertLoanWithTransaction(
        loan: LoanEntity,
        createLinkedTransaction: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
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

    suspend fun updateLoan(loan: LoanEntity) = withContext(Dispatchers.IO) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: LoanEntity) = withContext(Dispatchers.IO) {
        loanDao.deleteLoan(loan)
    }

    suspend fun deleteLoanById(id: Long) = withContext(Dispatchers.IO) {
        loanDao.deleteLoanById(id)
    }

    suspend fun recordLoanRepayment(
        loanId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanByIdDirect(loanId) ?: return@withContext
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
    }

    suspend fun markLoanSettled(loanId: Long) = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanByIdDirect(loanId) ?: return@withContext
        if (loan.remainingAmount > 0) {
            recordLoanRepayment(
                loanId = loanId,
                amount = loan.remainingAmount,
                note = "Full Settlement"
            )
        } else {
            loanDao.updateLoan(loan.copy(status = LoanStatus.SETTLED))
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
}
