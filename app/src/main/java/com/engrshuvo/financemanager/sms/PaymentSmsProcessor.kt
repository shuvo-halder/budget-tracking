package com.engrshuvo.financemanager.sms

import android.content.Context
import androidx.room.withTransaction
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.SmsImportStatus
import com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.sms.parser.PaymentSmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaymentSmsProcessor {

    suspend fun processManualSms(
        context: Context,
        sender: String,
        body: String,
        messageTimestamp: Long = System.currentTimeMillis()
    ): Boolean {
        return processIncomingSms(
            context = context,
            sender = sender,
            body = body,
            messageTimestamp = messageTimestamp,
            isManualOrShared = true
        )
    }

    suspend fun processIncomingSms(
        context: Context,
        sender: String,
        body: String,
        messageTimestamp: Long = System.currentTimeMillis(),
        isManualOrShared: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val prefsRepo = PaymentSmsPreferencesRepository(context)
        val prefs = prefsRepo.getPreferencesDirect()

        // 1. Strict Policy for automated checks: must be enabled and not pre-date activation
        if (!isManualOrShared) {
            if (!prefs.isEnabled) {
                return@withContext false
            }
            if (prefs.activatedAt <= 0L || messageTimestamp < prefs.activatedAt) {
                return@withContext false
            }
        }

        // 2. Parse SMS
        val parsed = PaymentSmsParser.parse(sender, body, messageTimestamp) ?: return@withContext false

        val database = AppDatabase.getDatabase(context)
        val smsDao = database.smsImportedRecordDao()
        val txDao = database.transactionDao()

        // 5. Deduplication Fingerprint
        val fallbackFingerprint = if (!parsed.trxId.isNullOrBlank()) {
            "TRX:${parsed.trxId}"
        } else {
            "${parsed.provider}:${parsed.type}:${parsed.amount}:${messageTimestamp / 60000}:${body.trim().hashCode()}"
        }

        // 6. Transactional Database Insertion and Deduplication
        val success = database.withTransaction {
            // Check existing TrxId
            if (!parsed.trxId.isNullOrBlank()) {
                val existing = smsDao.findByTrxId(parsed.trxId)
                if (existing != null) {
                    return@withTransaction false
                }
            } else {
                // Check fallback fingerprint for messages without TrxId
                val existingFingerprint = smsDao.findByFallbackFingerprint(fallbackFingerprint)
                if (existingFingerprint != null) {
                    return@withTransaction false
                }
            }

            // Candidate vs Auto-Post Determination
            val canAutoPost = prefs.autoPostUnambiguous && parsed.isConfident && !parsed.isCardBillPayment
            val initialStatus = if (canAutoPost) SmsImportStatus.POSTED else SmsImportStatus.PENDING_REVIEW

            // Insert into sms_imported_records FIRST
            val record = SmsImportedRecordEntity(
                sender = parsed.sender,
                provider = parsed.provider,
                trxId = parsed.trxId,
                type = parsed.type,
                amount = parsed.amount,
                fee = parsed.fee,
                counterparty = parsed.counterparty,
                suggestedCategory = parsed.suggestedCategory,
                timestamp = messageTimestamp,
                status = initialStatus,
                linkedTransactionId = null,
                fallbackFingerprint = fallbackFingerprint,
                note = parsed.note
            )

            val insertedId = smsDao.insertRecord(record)
            if (insertedId <= 0) {
                // Duplicate caught by SQLite UNIQUE index or failure
                return@withTransaction false
            }

            // Only create financial transaction if auto-post is eligible
            if (canAutoPost) {
                val category = CategoryCatalog.getCategoryById(parsed.suggestedCategory)
                val newTx = TransactionEntity(
                    type = parsed.type,
                    amount = parsed.amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    note = "${parsed.note}${if (parsed.trxId != null) " (TrxID: ${parsed.trxId})" else ""}",
                    timestamp = messageTimestamp
                )
                val createdTxId = txDao.insertTransaction(newTx)
                if (createdTxId > 0) {
                    smsDao.updateStatus(insertedId, SmsImportStatus.POSTED, createdTxId)
                } else {
                    throw IllegalStateException("Failed to insert linked transaction for SMS $insertedId")
                }
            }

            true
        }

        if (success) {
            prefsRepo.recordImportSuccess(messageTimestamp)
        }
        success
    }
}
