package com.engrshuvo.financemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncomeFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpenseFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :startTime")
    fun getMonthlyExpenseFlow(startTime: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoansFlow(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' ORDER BY startDate DESC")
    fun getActiveLoansFlow(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE type = :type ORDER BY startDate DESC")
    fun getLoansByTypeFlow(type: LoanType): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    fun getLoanByIdFlow(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanByIdDirect(id: Long): LoanEntity?

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM loans WHERE type = 'LENT' AND status = 'ACTIVE'")
    fun getTotalActiveLentFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM loans WHERE type = 'BORROWED' AND status = 'ACTIVE'")
    fun getTotalActiveBorrowedFlow(): Flow<Double>

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY timestamp DESC")
    fun getRepaymentsForLoanFlow(loanId: Long): Flow<List<LoanRepaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long

    @Query("DELETE FROM loan_repayments WHERE id = :repaymentId")
    suspend fun deleteRepayment(repaymentId: Long)
}

@Dao
interface BudgetSettingDao {
    @Query("SELECT * FROM budget_settings WHERE settingKey = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<BudgetSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: BudgetSettingEntity)
}

@Dao
interface BudgetAllocationDao {
    @Query("SELECT * FROM budget_allocations WHERE monthKey = :monthKey ORDER BY allocatedAmount DESC")
    fun getAllocationsForMonth(monthKey: String): Flow<List<BudgetAllocationEntity>>

    @Query("SELECT * FROM budget_allocations ORDER BY monthKey DESC")
    fun getAllAllocations(): Flow<List<BudgetAllocationEntity>>

    @Query("SELECT * FROM budget_allocations WHERE monthKey = :monthKey")
    suspend fun getAllocationsForMonthDirect(monthKey: String): List<BudgetAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAllocation(allocation: BudgetAllocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAllocations(allocations: List<BudgetAllocationEntity>)

    @Query("DELETE FROM budget_allocations WHERE monthKey = :monthKey AND categoryId = :categoryId")
    suspend fun deleteAllocation(monthKey: String, categoryId: String)

    @Query("DELETE FROM budget_allocations WHERE monthKey = :monthKey")
    suspend fun deleteAllAllocationsForMonth(monthKey: String)
}

