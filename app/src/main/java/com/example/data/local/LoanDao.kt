package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.LoanEntity
import com.example.data.model.LoanRepaymentEntity
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import kotlinx.coroutines.flow.Flow

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
