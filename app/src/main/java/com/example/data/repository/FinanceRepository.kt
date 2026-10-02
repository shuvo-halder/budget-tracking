package com.example.data.repository

import com.example.data.local.BudgetSettingDao
import com.example.data.local.LoanDao
import com.example.data.local.TransactionDao
import com.example.data.model.BudgetSettingEntity
import com.example.data.model.LoanEntity
import com.example.data.model.LoanRepaymentEntity
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetSettingDao: BudgetSettingDao,
    private val loanDao: LoanDao
) {
    // Transaction streams
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val totalIncome: Flow<Double> = transactionDao.getTotalIncomeFlow()
    val totalExpense: Flow<Double> = transactionDao.getTotalExpenseFlow()

    // Loan streams
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoansFlow()
    val activeLoans: Flow<List<LoanEntity>> = loanDao.getActiveLoansFlow()
    val totalActiveLent: Flow<Double> = loanDao.getTotalActiveLentFlow()
    val totalActiveBorrowed: Flow<Double> = loanDao.getTotalActiveBorrowedFlow()

    // Budget Limit stream
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
        // If it was linked to a loan, delete loan too
        if (transaction.loanId != null) {
            loanDao.deleteLoanById(transaction.loanId)
        }
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun insertLoanWithTransaction(
        loan: LoanEntity,
        createLinkedTransaction: Boolean = true
    ): Long {
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
        return loanId
    }

    suspend fun updateLoan(loan: LoanEntity) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: LoanEntity) {
        loanDao.deleteLoan(loan)
    }

    suspend fun deleteLoanById(id: Long) {
        loanDao.deleteLoanById(id)
    }

    suspend fun recordLoanRepayment(
        loanId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val loan = loanDao.getLoanByIdDirect(loanId) ?: return
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

        // Also record a corresponding transaction for balance reflection:
        // If we lent money and received repayment -> INCOME
        // If we borrowed money and made repayment -> EXPENSE
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

    suspend fun markLoanSettled(loanId: Long) {
        val loan = loanDao.getLoanByIdDirect(loanId) ?: return
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
        return loanDao.getRepaymentsForLoanFlow(loanId)
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
