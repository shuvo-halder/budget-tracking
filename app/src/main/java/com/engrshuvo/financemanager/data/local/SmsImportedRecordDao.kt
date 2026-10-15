package com.engrshuvo.financemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.engrshuvo.financemanager.data.model.SmsImportStatus
import com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsImportedRecordDao {

    @Query("SELECT * FROM sms_imported_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<SmsImportedRecordEntity>>

    @Query("SELECT * FROM sms_imported_records WHERE status = 'PENDING_REVIEW' ORDER BY timestamp DESC")
    fun getPendingReviewRecordsFlow(): Flow<List<SmsImportedRecordEntity>>

    @Query("SELECT * FROM sms_imported_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): SmsImportedRecordEntity?

    @Query("SELECT * FROM sms_imported_records WHERE trxId IS NOT NULL AND trxId = :trxId LIMIT 1")
    suspend fun findByTrxId(trxId: String): SmsImportedRecordEntity?

    @Query("SELECT * FROM sms_imported_records WHERE fallbackFingerprint = :fingerprint LIMIT 1")
    suspend fun findByFallbackFingerprint(fingerprint: String): SmsImportedRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: SmsImportedRecordEntity): Long

    @Update
    suspend fun updateRecord(record: SmsImportedRecordEntity): Int

    @Query("UPDATE sms_imported_records SET status = :status, linkedTransactionId = :linkedTxId WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SmsImportStatus, linkedTxId: Long? = null): Int

    @Query("DELETE FROM sms_imported_records WHERE id = :id")
    suspend fun deleteRecord(id: Long): Int

    @Query("SELECT COUNT(*) FROM sms_imported_records")
    suspend fun getImportedCount(): Int

    @Query("SELECT * FROM sms_imported_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsDirect(): List<SmsImportedRecordEntity>
}
