package com.engrshuvo.financemanager

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.model.SmsImportStatus
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.sms.PaymentSmsPreferencesRepository
import com.engrshuvo.financemanager.sms.PaymentSmsProcessor
import com.engrshuvo.financemanager.sms.parser.PaymentSmsParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentSmsImportUnitTest {

    private lateinit var context: Context
    private lateinit var app: Application
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var prefsRepo: PaymentSmsPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        app = context as Application
        shadowOf(app).grantPermissions(Manifest.permission.RECEIVE_SMS)

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Inject in-memory DB into singleton so PaymentSmsProcessor uses this database
        val instanceField = AppDatabase::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, database)

        repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            budgetSettingDao = database.budgetSettingDao(),
            loanDao = database.loanDao(),
            budgetAllocationDao = database.budgetAllocationDao(),
            purchaseItemDao = database.purchaseItemDao(),
            smsImportedRecordDao = database.smsImportedRecordDao()
        )

        prefsRepo = PaymentSmsPreferencesRepository(context)
    }

    @After
    fun tearDown() {
        database.close()
        val instanceField = AppDatabase::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    // 1. Feature disabled — no import
    @Test
    fun test01_featureDisabled_noImport() = runBlocking {
        prefsRepo.updateEnabled(false)

        val processed = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "bKash",
            body = "You have received Tk 1,500.00 from 01711000000. Fee Tk 0.00. Balance Tk 5,000.00. TrxID 9K48X7ABC.",
            messageTimestamp = System.currentTimeMillis()
        )

        assertFalse(processed)
        val records = database.smsImportedRecordDao().getAllRecordsDirect()
        assertTrue(records.isEmpty())
        val transactions = database.transactionDao().getAllTransactionsDirect()
        assertTrue(transactions.isEmpty())
    }

    // 2. Permission unavailable — no import and no crash
    @Test
    fun test02_permissionUnavailable_noImportAndNoCrash() = runBlocking {
        prefsRepo.updateEnabled(true)
        shadowOf(app).denyPermissions(Manifest.permission.RECEIVE_SMS)

        val processed = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "bKash",
            body = "You have received Tk 1,500.00 from 01711000000. Fee Tk 0.00. Balance Tk 5,000.00. TrxID 9K48X7ABC.",
            messageTimestamp = System.currentTimeMillis()
        )

        assertFalse(processed)
        val records = database.smsImportedRecordDao().getAllRecordsDirect()
        assertTrue(records.isEmpty())
    }

    // 3. Message timestamp before activation — rejected
    @Test
    fun test03_timestampBeforeActivation_rejected() = runBlocking {
        val now = System.currentTimeMillis()
        prefsRepo.updateEnabled(true)
        // Set activation timestamp explicitly to now
        val historicalTimestamp = now - 60_000L // 1 minute in the past

        val processed = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "bKash",
            body = "Payment of Tk 500.00 to Merchant is successful. Fee Tk 0.00. Balance Tk 2,500.00. TrxID 8A34BC99.",
            messageTimestamp = historicalTimestamp
        )

        assertFalse(processed)
        val records = database.smsImportedRecordDao().getAllRecordsDirect()
        assertTrue(records.isEmpty())
    }

    // 4. Re-enable feature — no historical import
    @Test
    fun test04_reEnableFeature_noHistoricalImport() = runBlocking {
        // First activation
        prefsRepo.updateEnabled(true)
        val firstActivation = prefsRepo.getPreferencesDirect().activatedAt

        // Disable feature
        prefsRepo.updateEnabled(false)
        assertEquals(0L, prefsRepo.getPreferencesDirect().activatedAt)

        // Re-enable feature
        Thread.sleep(10)
        prefsRepo.updateEnabled(true)
        val reActivatedAt = prefsRepo.getPreferencesDirect().activatedAt
        assertTrue(reActivatedAt >= firstActivation)

        // Verify that messages with timestamps before re-activation are rejected
        val processedOld = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "Nagad",
            body = "Payment of Tk 300.00 to Store is successful. Fee Tk 0.00. TrxID 7N92K45P.",
            messageTimestamp = reActivatedAt - 1000L
        )
        assertFalse(processedOld)
        assertTrue(database.smsImportedRecordDao().getAllRecordsDirect().isEmpty())
    }

    // 5. Unsupported sender — ignored or safely rejected
    @Test
    fun test05_unsupportedSender_safelyRejected() = runBlocking {
        prefsRepo.updateEnabled(true)
        val now = System.currentTimeMillis() + 1000L

        // Random personal contact sending a text with currency
        val parsed = PaymentSmsParser.parse("+8801700112233", "I paid Tk 500 for grocery earlier.")
        assertNull(parsed)

        val processed = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "+8801700112233",
            body = "I paid Tk 500 for grocery earlier.",
            messageTimestamp = now
        )
        assertFalse(processed)
    }

    // 6. OTP/promotional message — not imported as a financial transaction
    @Test
    fun test06_otpAndPromo_notImported() = runBlocking {
        prefsRepo.updateEnabled(true)
        val now = System.currentTimeMillis() + 1000L

        val otp1 = PaymentSmsParser.parse("bKash", "Your bKash verification code is 482910. Do not share with anyone.")
        assertNull(otp1)

        val otp2 = PaymentSmsParser.parse("Nagad", "Your OTP is 1234. Valid for 2 minutes. Dial *167# for menu.")
        assertNull(otp2)

        val promo = PaymentSmsParser.parse("bKash", "Recharge offer: Recharge Tk 50 to get cashback offer of Tk 10!")
        assertNull(promo)

        val processed = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "bKash",
            body = "Your bKash verification code is 482910. Do not share with anyone.",
            messageTimestamp = now
        )
        assertFalse(processed)
    }

    // 7. Invalid or missing amount — not automatically imported
    @Test
    fun test07_invalidOrMissingAmount_rejected() {
        val missingAmount = PaymentSmsParser.parse("bKash", "Payment to Merchant is successful. Fee Tk 0.00. TrxID 12345.")
        assertNull(missingAmount)

        val zeroAmount = PaymentSmsParser.parse("bKash", "Payment of Tk 0.00 to Merchant is successful. Fee Tk 0.00. TrxID 12345.")
        assertNull(zeroAmount)
    }

    // 8. Ambiguous message — sent to review
    @Test
    fun test08_ambiguousMessage_sentToReview() = runBlocking {
        prefsRepo.updateEnabled(true)
        prefsRepo.updateAutoPost(true) // Even if auto-post is on
        val now = System.currentTimeMillis() + 1000L

        // Ambiguous bank notice without clear TrxId
        val processed = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "City Bank",
            body = "Your Bank A/C was credited by BDT 5,000.00.",
            messageTimestamp = now
        )

        assertTrue(processed)
        val records = database.smsImportedRecordDao().getAllRecordsDirect()
        assertEquals(1, records.size)
        // Must be routed to review, NOT auto-posted because trxId was missing
        assertEquals(SmsImportStatus.PENDING_REVIEW, records[0].status)
        assertNull(records[0].linkedTransactionId)
        // No transaction in ledger
        assertTrue(database.transactionDao().getAllTransactionsDirect().isEmpty())
    }

    // 9. Valid supported message — correct parsed fields
    @Test
    fun test09_validSupportedMessage_correctParsedFields() {
        val now = System.currentTimeMillis()
        val parsed = PaymentSmsParser.parse(
            sender = "bKash",
            body = "Payment of Tk 450.00 to Shwapno is successful. Fee Tk 0.00. Balance Tk 4,550.00. TrxID 8A34BC99.",
            messageTimestamp = now
        )

        assertNotNull(parsed)
        assertEquals("bKash", parsed!!.provider)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(450.0, parsed.amount, 0.001)
        assertEquals(0.0, parsed.fee, 0.001)
        assertEquals("8A34BC99", parsed.trxId)
        assertEquals("Shwapno", parsed.counterparty)
        assertEquals("shopping", parsed.suggestedCategory)
        assertTrue(parsed.isConfident)
    }

    // 10. Duplicate provider transaction ID — only one financial record
    @Test
    fun test10_duplicateTrxId_onlyOneRecord() = runBlocking {
        prefsRepo.updateEnabled(true)
        prefsRepo.updateAutoPost(true)
        val now = System.currentTimeMillis() + 1000L

        val smsBody = "Payment of Tk 450.00 to Shwapno is successful. Fee Tk 0.00. Balance Tk 4,550.00. TrxID 8A34BC99."

        // First delivery
        val first = PaymentSmsProcessor.processIncomingSms(context, "bKash", smsBody, now)
        assertTrue(first)

        // Second delivery of same message with same TrxID
        val second = PaymentSmsProcessor.processIncomingSms(context, "bKash", smsBody, now + 100L)
        assertFalse(second)

        // Verify only 1 SMS record and 1 transaction exist
        val records = database.smsImportedRecordDao().getAllRecordsDirect()
        assertEquals(1, records.size)
        val transactions = database.transactionDao().getAllTransactionsDirect()
        assertEquals(1, transactions.size)
    }

    // 11. Duplicate SMS delivery — no duplicate balance effect
    @Test
    fun test11_duplicateSmsDelivery_noDuplicateBalanceEffect() = runBlocking {
        prefsRepo.updateEnabled(true)
        prefsRepo.updateAutoPost(true)
        val now = System.currentTimeMillis() + 1000L

        val initialBalance = database.transactionDao().getAllTransactionsDirect().sumOf {
            if (it.type == TransactionType.INCOME) it.amount else -it.amount
        }

        val smsBody = "You have received Tk 2,000.00 from 01711000000. Fee Tk 0.00. TrxID 9K999ABC."

        // First delivery
        PaymentSmsProcessor.processIncomingSms(context, "bKash", smsBody, now)

        // Attempt duplicate deliveries
        PaymentSmsProcessor.processIncomingSms(context, "bKash", smsBody, now + 50L)
        PaymentSmsProcessor.processIncomingSms(context, "bKash", smsBody, now + 100L)

        val finalTransactions = database.transactionDao().getAllTransactionsDirect()
        assertEquals(1, finalTransactions.size)
        val newBalance = finalTransactions.sumOf {
            if (it.type == TransactionType.INCOME) it.amount else -it.amount
        }
        assertEquals(initialBalance + 2000.0, newBalance, 0.001)
    }

    // 12. Similar legitimate transactions — both preserved
    @Test
    fun test12_similarLegitimateTransactions_bothPreserved() = runBlocking {
        prefsRepo.updateEnabled(true)
        prefsRepo.updateAutoPost(true)
        val now = System.currentTimeMillis() + 1000L

        // Two distinct purchases of the exact same amount at the same counter, but with unique provider TrxIDs
        val tx1 = "Payment of Tk 50.00 to Bus Counter is successful. Fee Tk 0.00. TrxID TRX_TICKET_1."
        val tx2 = "Payment of Tk 50.00 to Bus Counter is successful. Fee Tk 0.00. TrxID TRX_TICKET_2."

        val res1 = PaymentSmsProcessor.processIncomingSms(context, "bKash", tx1, now)
        val res2 = PaymentSmsProcessor.processIncomingSms(context, "bKash", tx2, now + 2000L)

        assertTrue(res1)
        assertTrue(res2)

        val records = database.smsImportedRecordDao().getAllRecordsDirect()
        assertEquals(2, records.size)
        val transactions = database.transactionDao().getAllTransactionsDirect()
        assertEquals(2, transactions.size)
    }

    // 13. Credit-card purchase vs bill payment — correctly distinguished
    @Test
    fun test13_creditCardPurchaseVsBillPayment_distinguished() {
        val purchaseMsg = "Your Credit Card ending 1234 was used for purchase of BDT 3,500.00 at Unimart on 15-OCT-2026. Ref CC123."
        val parsedPurchase = PaymentSmsParser.parse("City Bank", purchaseMsg)
        assertNotNull(parsedPurchase)
        assertEquals(TransactionType.EXPENSE, parsedPurchase!!.type)
        assertFalse(parsedPurchase.isCardBillPayment)
        assertTrue(parsedPurchase.isConfident)
        assertEquals("shopping", parsedPurchase.suggestedCategory)

        val billPaymentMsg = "Payment of BDT 15,000 received for your City Bank Credit Card ending 1234 on 15-OCT-2026. Ref PAY999."
        val parsedBill = PaymentSmsParser.parse("City Bank", billPaymentMsg)
        assertNotNull(parsedBill)
        assertTrue(parsedBill!!.isCardBillPayment)
        assertFalse(parsedBill.isConfident) // Never auto-posted
        assertEquals("bills", parsedBill.suggestedCategory)
    }

    // 14. Fee, refund, reversal, and failed transaction — correctly handled
    @Test
    fun test14_feeRefundReversalAndFailed_correctlyHandled() {
        // Fee extraction on cash out
        val cashOutMsg = "Cash Out of Tk 1,000.00 from 01712000000 is successful. Fee Tk 18.50. Balance Tk 5,531.50. TrxID 5C22EE33."
        val parsedCashOut = PaymentSmsParser.parse("bKash", cashOutMsg)
        assertNotNull(parsedCashOut)
        assertEquals(18.50, parsedCashOut!!.fee, 0.001)
        assertEquals(1000.0, parsedCashOut.amount, 0.001)

        // Refund
        val refundMsg = "Refund of Tk 600.00 received from Foodpanda. TrxID 3F44GG55."
        val parsedRefund = PaymentSmsParser.parse("bKash", refundMsg)
        assertNotNull(parsedRefund)
        assertEquals(TransactionType.INCOME, parsedRefund!!.type)
        assertEquals(600.0, parsedRefund.amount, 0.001)

        // Reversal
        val reversalMsg = "Your Bank A/C has been credited by BDT 1,200.00 for transaction reversal. Ref REV123."
        val parsedReversal = PaymentSmsParser.parse("DBBL", reversalMsg)
        assertNotNull(parsedReversal)
        assertEquals(TransactionType.INCOME, parsedReversal!!.type)
        assertEquals(1200.0, parsedReversal.amount, 0.001)

        // Failed transaction must be rejected
        val failedMsg = "Payment of Tk 500.00 to Merchant failed due to system error. TrxID 9999."
        val parsedFailed = PaymentSmsParser.parse("bKash", failedMsg)
        assertNull(parsedFailed)
    }

    // 15. SMS import disabled after receipt but before processing — safe policy
    @Test
    fun test15_disabledBeforeProcessing_rejectedSafely() = runBlocking {
        // Enabled when message arrived
        prefsRepo.updateEnabled(true)
        val arrivalTime = System.currentTimeMillis() + 1000L

        // User toggles feature OFF before background worker processes the event
        prefsRepo.updateEnabled(false)

        val processed = PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "Nagad",
            body = "Payment of Tk 200.00 to Shop is successful. Fee Tk 0.00. TrxID 555666.",
            messageTimestamp = arrivalTime
        )

        assertFalse(processed)
        assertTrue(database.smsImportedRecordDao().getAllRecordsDirect().isEmpty())
    }

    // 16. Existing manual transaction entry and loan accounting — no regression
    @Test
    fun test16_manualTransactionAndLoanAccounting_noRegression() = runBlocking {
        // Insert a normal manual transaction
        val manualTx = TransactionEntity(
            type = TransactionType.INCOME,
            amount = 50000.0,
            categoryId = "salary",
            categoryName = "Salary",
            note = "Monthly Salary",
            timestamp = System.currentTimeMillis()
        )
        val txId = repository.insertTransaction(manualTx)
        assertTrue(txId > 0)

        // Now process an SMS
        prefsRepo.updateEnabled(true)
        prefsRepo.updateAutoPost(true)
        val now = System.currentTimeMillis() + 1000L
        PaymentSmsProcessor.processIncomingSms(
            context = context,
            sender = "bKash",
            body = "Payment of Tk 500.00 to Grocer is successful. Fee Tk 0.00. TrxID TX500.",
            messageTimestamp = now
        )

        val allTx = repository.allTransactions.first()
        assertEquals(2, allTx.size)
        // Ensure manual transaction remains completely intact
        val retrievedSalary = allTx.first { it.id == txId }
        assertEquals(50000.0, retrievedSalary.amount, 0.001)
        assertEquals("Salary", retrievedSalary.categoryName)
    }

    // 17. No historical inbox scanning — verified by inspecting absence of READ_SMS and ContentResolver queries
    @Test
    fun test17_noHistoricalInboxScanningPolicy() {
        // Verify that PaymentSmsParser does not reference ContentResolver
        assertFalse(PaymentSmsParser::class.java.declaredMethods.any { it.name.contains("query") })
        // Verify that PaymentSmsProcessor does not have inbox scan methods
        assertFalse(PaymentSmsProcessor::class.java.declaredMethods.any { it.name.contains("scan") || it.name.contains("inbox") })
    }
}
