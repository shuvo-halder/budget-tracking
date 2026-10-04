package com.engrshuvo.financemanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.local.BudgetAllocationDao
import com.engrshuvo.financemanager.data.local.BudgetSettingDao
import com.engrshuvo.financemanager.data.local.LoanDao
import com.engrshuvo.financemanager.data.local.TransactionDao
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.FinanceBackupData
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreDataIntegrityTest {

    private lateinit var context: Context
    private lateinit var sourceDb: AppDatabase
    private lateinit var sourceRepo: FinanceRepository
    private lateinit var targetDb: AppDatabase
    private lateinit var targetRepo: FinanceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sourceDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sourceRepo = FinanceRepository(
            transactionDao = sourceDb.transactionDao(),
            budgetSettingDao = sourceDb.budgetSettingDao(),
            loanDao = sourceDb.loanDao(),
            budgetAllocationDao = sourceDb.budgetAllocationDao(),
            database = sourceDb
        )

        targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        targetRepo = FinanceRepository(
            transactionDao = targetDb.transactionDao(),
            budgetSettingDao = targetDb.budgetSettingDao(),
            loanDao = targetDb.loanDao(),
            budgetAllocationDao = targetDb.budgetAllocationDao(),
            database = targetDb
        )
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
    }

    // ==========================================
    // TASK 1: Complete Backup Round-Trip Test
    // ==========================================

    @Test
    fun `complete backup round-trip preserves all entities, relationships, timestamps and metadata`() = runBlocking {
        val now = System.currentTimeMillis()
        val archiveTs = now - (5 * 24 * 60 * 60 * 1000L) // 5 days ago

        // 1. Populate source database with loans (active, settled, archived)
        val loan1 = LoanEntity(
            id = 101L,
            type = LoanType.LENT,
            personName = "Tareq Hasan",
            phoneNumber = "01711223344",
            initialAmount = 15000.0,
            remainingAmount = 9000.0,
            status = LoanStatus.ACTIVE,
            startDate = now - (30 * 24 * 60 * 60 * 1000L),
            dueDate = now + (60 * 24 * 60 * 60 * 1000L),
            note = "Emergency loan for laptop repair",
            archivedAt = null
        )
        val loan2 = LoanEntity(
            id = 102L,
            type = LoanType.BORROWED,
            personName = "City Bank",
            phoneNumber = "16234",
            initialAmount = 50000.0,
            remainingAmount = 0.0,
            status = LoanStatus.SETTLED,
            startDate = now - (90 * 24 * 60 * 60 * 1000L),
            dueDate = null,
            note = "Short term debt",
            archivedAt = null
        )
        val loan3Archived = LoanEntity(
            id = 103L,
            type = LoanType.LENT,
            personName = "Old Friend",
            phoneNumber = "",
            initialAmount = 3000.0,
            remainingAmount = 0.0,
            status = LoanStatus.SETTLED,
            startDate = now - (120 * 24 * 60 * 60 * 1000L),
            dueDate = null,
            note = "Settled and archived loan",
            archivedAt = archiveTs
        )
        sourceDb.loanDao().insertAllLoans(listOf(loan1, loan2, loan3Archived))

        // 2. Populate loan repayments
        val rep1 = LoanRepaymentEntity(
            id = 201L,
            loanId = 101L,
            amount = 6000.0,
            note = "First installment cash",
            timestamp = now - (15 * 24 * 60 * 60 * 1000L),
            archivedAt = null
        )
        val rep2 = LoanRepaymentEntity(
            id = 202L,
            loanId = 102L,
            amount = 50000.0,
            note = "Full clearance payment",
            timestamp = now - (10 * 24 * 60 * 60 * 1000L),
            archivedAt = null
        )
        val rep3Archived = LoanRepaymentEntity(
            id = 203L,
            loanId = 103L,
            amount = 3000.0,
            note = "Old archive repayment",
            timestamp = now - (110 * 24 * 60 * 60 * 1000L),
            archivedAt = archiveTs
        )
        sourceDb.loanDao().insertAllRepayments(listOf(rep1, rep2, rep3Archived))

        // 3. Populate transactions (Income, Expense, Loan-linked, Archived)
        val tx1 = TransactionEntity(
            id = 301L,
            type = TransactionType.INCOME,
            amount = 85000.0,
            categoryId = "salary",
            categoryName = "Monthly Salary",
            note = "Corporate Salary Oct",
            timestamp = now - (4 * 24 * 60 * 60 * 1000L),
            loanId = null,
            archivedAt = null
        )
        val tx2 = TransactionEntity(
            id = 302L,
            type = TransactionType.EXPENSE,
            amount = 18000.0,
            categoryId = "housing",
            categoryName = "House Rent",
            note = "Apartment Rent",
            timestamp = now - (3 * 24 * 60 * 60 * 1000L),
            loanId = null,
            archivedAt = null
        )
        val tx3LoanLinked = TransactionEntity(
            id = 303L,
            type = TransactionType.LOAN,
            amount = 15000.0,
            categoryId = "loan_lent",
            categoryName = "Loan to Tareq Hasan",
            note = "Lent to Tareq",
            timestamp = loan1.startDate,
            loanId = 101L,
            archivedAt = null
        )
        val tx4RepaymentCollected = TransactionEntity(
            id = 304L,
            type = TransactionType.INCOME,
            amount = 6000.0,
            categoryId = "loan_collected",
            categoryName = "Repayment from Tareq Hasan",
            note = "Cash installment",
            timestamp = rep1.timestamp,
            loanId = 101L,
            archivedAt = null
        )
        val tx5Archived = TransactionEntity(
            id = 305L,
            type = TransactionType.EXPENSE,
            amount = 1200.0,
            categoryId = "food",
            categoryName = "Food & Dining",
            note = "Old dinner receipt",
            timestamp = now - (20 * 24 * 60 * 60 * 1000L),
            loanId = null,
            archivedAt = archiveTs
        )
        sourceDb.transactionDao().insertAllTransactions(listOf(tx1, tx2, tx3LoanLinked, tx4RepaymentCollected, tx5Archived))

        // 4. Populate Budget Settings
        val settingMonthly = BudgetSettingEntity(
            settingKey = BudgetSettingEntity.KEY_MONTHLY_BUDGET,
            amountLimit = 45000.0,
            currencyCode = "BDT"
        )
        val settingDaily = BudgetSettingEntity(
            settingKey = BudgetSettingEntity.KEY_DAILY_BUDGET,
            amountLimit = 1500.0,
            currencyCode = "BDT"
        )
        sourceDb.budgetSettingDao().insertAllSettings(listOf(settingMonthly, settingDaily))

        // 5. Populate Budget Allocations
        val alloc1 = BudgetAllocationEntity(
            monthKey = "2026-10",
            categoryId = "housing",
            categoryName = "House Rent",
            allocatedAmount = 18000.0,
            updatedAt = now
        )
        val alloc2 = BudgetAllocationEntity(
            monthKey = "2026-10",
            categoryId = "daily_expenses",
            categoryName = "Daily Living",
            allocatedAmount = 12000.0,
            updatedAt = now
        )
        val alloc3 = BudgetAllocationEntity(
            monthKey = "2026-11",
            categoryId = "housing",
            categoryName = "House Rent",
            allocatedAmount = 18000.0,
            updatedAt = now
        )
        sourceDb.budgetAllocationDao().insertOrUpdateAllocations(listOf(alloc1, alloc2, alloc3))

        // 6. Export database using production serialization path
        val exportedBackup = sourceRepo.exportBackupData()
        assertEquals(FinanceBackupData.BACKUP_FORMAT_IDENTIFIER, exportedBackup.backupFormat)
        assertEquals(FinanceBackupData.CURRENT_BACKUP_VERSION, exportedBackup.backupVersion)
        assertEquals(3, exportedBackup.databaseVersion)
        assertEquals(3, exportedBackup.loans.size)
        assertEquals(3, exportedBackup.loanRepayments.size)
        assertEquals(5, exportedBackup.transactions.size)
        assertEquals(2, exportedBackup.budgetSettings.size)
        assertEquals(3, exportedBackup.budgetAllocations.size)

        val jsonString = exportedBackup.toJsonString()
        assertTrue("JSON must contain backupFormat", jsonString.contains(FinanceBackupData.BACKUP_FORMAT_IDENTIFIER))

        // 7. Parse JSON using production parser
        val parsedBackup = FinanceBackupData.fromJsonString(jsonString)
        assertEquals(exportedBackup.backupFormat, parsedBackup.backupFormat)
        assertEquals(exportedBackup.backupVersion, parsedBackup.backupVersion)
        assertEquals(exportedBackup.databaseVersion, parsedBackup.databaseVersion)

        // 8. Restore into target database using production restore path
        val restoreResult = targetRepo.restoreBackupData(parsedBackup)
        assertTrue(restoreResult.isSuccess)
        val totalRestoredCount = restoreResult.getOrNull()
        assertEquals(14, totalRestoredCount) // 5 txs + 3 loans + 3 repayments + 3 allocations

        // 9. Field-by-field verification of restored records in target database
        val restoredLoans = targetDb.loanDao().getAllLoansForBackup()
        assertEquals(3, restoredLoans.size)
        val rLoan1 = restoredLoans.find { it.id == 101L }!!
        assertEquals(loan1.type, rLoan1.type)
        assertEquals(loan1.personName, rLoan1.personName)
        assertEquals(loan1.phoneNumber, rLoan1.phoneNumber)
        assertEquals(loan1.initialAmount, rLoan1.initialAmount, 0.001)
        assertEquals(loan1.remainingAmount, rLoan1.remainingAmount, 0.001)
        assertEquals(loan1.status, rLoan1.status)
        assertEquals(loan1.startDate, rLoan1.startDate)
        assertEquals(loan1.dueDate, rLoan1.dueDate)
        assertEquals(loan1.note, rLoan1.note)
        assertNull(rLoan1.archivedAt)

        val rLoan3 = restoredLoans.find { it.id == 103L }!!
        assertEquals(archiveTs, rLoan3.archivedAt)

        val restoredRepayments = targetDb.loanDao().getAllRepaymentsForBackup()
        assertEquals(3, restoredRepayments.size)
        val rRep1 = restoredRepayments.find { it.id == 201L }!!
        assertEquals(101L, rRep1.loanId)
        assertEquals(6000.0, rRep1.amount, 0.001)
        assertEquals("First installment cash", rRep1.note)
        assertNull(rRep1.archivedAt)

        val rRep3 = restoredRepayments.find { it.id == 203L }!!
        assertEquals(archiveTs, rRep3.archivedAt)

        val restoredTxs = targetDb.transactionDao().getAllTransactionsForBackup()
        assertEquals(5, restoredTxs.size)
        val rTx3 = restoredTxs.find { it.id == 303L }!!
        assertEquals(TransactionType.LOAN, rTx3.type)
        assertEquals(15000.0, rTx3.amount, 0.001)
        assertEquals(101L, rTx3.loanId)
        assertNull(rTx3.archivedAt)

        val rTx5 = restoredTxs.find { it.id == 305L }!!
        assertEquals(archiveTs, rTx5.archivedAt)

        val restoredSettings = targetDb.budgetSettingDao().getAllSettingsForBackup()
        assertEquals(2, restoredSettings.size)
        val rMonthly = restoredSettings.find { it.settingKey == BudgetSettingEntity.KEY_MONTHLY_BUDGET }!!
        assertEquals(45000.0, rMonthly.amountLimit, 0.001)
        val rDaily = restoredSettings.find { it.settingKey == BudgetSettingEntity.KEY_DAILY_BUDGET }!!
        assertEquals(1500.0, rDaily.amountLimit, 0.001)

        val restoredAllocations = targetDb.budgetAllocationDao().getAllAllocationsDirect()
        assertEquals(3, restoredAllocations.size)
        val rAlloc1 = restoredAllocations.find { it.monthKey == "2026-10" && it.categoryId == "housing" }!!
        assertEquals(18000.0, rAlloc1.allocatedAmount, 0.001)
        assertEquals("House Rent", rAlloc1.categoryName)
    }

    // ==========================================
    // TASK 2: Atomic Restore Rollback Test
    // ==========================================

    private class FaultInjectingTransactionDao(
        private val delegate: TransactionDao,
        var shouldThrowOnInsertAll: Boolean = false
    ) : TransactionDao by delegate {
        override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) {
            if (shouldThrowOnInsertAll) {
                throw IOException("SIMULATED DISK WRITE FAILURE IN TRANSACTION DAO")
            }
            delegate.insertAllTransactions(transactions)
        }
    }

    @Test
    fun `deliberate failure inside restore transaction completely rolls back and preserves pre-restore records`() = runBlocking {
        val now = System.currentTimeMillis()
        val archiveTs = now - 500000L

        // 1. Seed database with initial baseline data
        val preLoan = LoanEntity(
            id = 55L,
            type = LoanType.LENT,
            personName = "Existing Loan Person",
            initialAmount = 10000.0,
            remainingAmount = 8000.0,
            status = LoanStatus.ACTIVE,
            startDate = now,
            archivedAt = null
        )
        targetDb.loanDao().insertLoan(preLoan)

        val preRep = LoanRepaymentEntity(
            id = 77L,
            loanId = 55L,
            amount = 2000.0,
            note = "Pre-existing repayment",
            timestamp = now,
            archivedAt = null
        )
        targetDb.loanDao().insertRepayment(preRep)

        val preTx = TransactionEntity(
            id = 99L,
            type = TransactionType.INCOME,
            amount = 25000.0,
            categoryId = "salary",
            categoryName = "Pre-existing Salary",
            note = "Pre-existing note",
            timestamp = now,
            loanId = null,
            archivedAt = archiveTs
        )
        targetDb.transactionDao().insertTransaction(preTx)

        val preSetting = BudgetSettingEntity(
            settingKey = BudgetSettingEntity.KEY_MONTHLY_BUDGET,
            amountLimit = 35000.0,
            currencyCode = "BDT"
        )
        targetDb.budgetSettingDao().insertOrUpdateSetting(preSetting)

        val preAlloc = BudgetAllocationEntity(
            monthKey = "2026-10",
            categoryId = "family",
            categoryName = "Family Support",
            allocatedAmount = 10000.0,
            updatedAt = now
        )
        targetDb.budgetAllocationDao().insertOrUpdateAllocation(preAlloc)

        // Capture complete snapshot before restore
        val initialLoans = targetDb.loanDao().getAllLoansForBackup()
        val initialRepayments = targetDb.loanDao().getAllRepaymentsForBackup()
        val initialTxs = targetDb.transactionDao().getAllTransactionsForBackup()
        val initialSettings = targetDb.budgetSettingDao().getAllSettingsForBackup()
        val initialAllocs = targetDb.budgetAllocationDao().getAllAllocationsDirect()

        assertEquals(1, initialLoans.size)
        assertEquals(1, initialRepayments.size)
        assertEquals(1, initialTxs.size)
        assertEquals(1, initialSettings.size)
        assertEquals(1, initialAllocs.size)

        // 2. Create Fault-Injecting Repository wrapping real AppDatabase
        val faultDao = FaultInjectingTransactionDao(targetDb.transactionDao(), shouldThrowOnInsertAll = true)
        val faultRepo = FinanceRepository(
            transactionDao = faultDao,
            budgetSettingDao = targetDb.budgetSettingDao(),
            loanDao = targetDb.loanDao(),
            budgetAllocationDao = targetDb.budgetAllocationDao(),
            database = targetDb // Real Room Database with withTransaction
        )

        // 3. Construct candidate backup data to restore
        val candidateData = FinanceBackupData(
            backupFormat = FinanceBackupData.BACKUP_FORMAT_IDENTIFIER,
            backupVersion = 1,
            loans = listOf(
                LoanEntity(id = 888L, type = LoanType.BORROWED, personName = "New Person", initialAmount = 5000.0, remainingAmount = 5000.0)
            ),
            loanRepayments = listOf(
                LoanRepaymentEntity(id = 999L, loanId = 888L, amount = 1000.0)
            ),
            transactions = listOf(
                TransactionEntity(id = 777L, type = TransactionType.EXPENSE, amount = 400.0, categoryId = "food", categoryName = "Food", note = "Lunch")
            ),
            budgetSettings = listOf(
                BudgetSettingEntity(settingKey = BudgetSettingEntity.KEY_MONTHLY_BUDGET, amountLimit = 99000.0)
            ),
            budgetAllocations = listOf(
                BudgetAllocationEntity(monthKey = "2026-12", categoryId = "food", categoryName = "Food", allocatedAmount = 5000.0)
            )
        )

        // 4. Attempt restoration (will fail on insertAllTransactions inside withTransaction)
        val result = faultRepo.restoreBackupData(candidateData)
        assertTrue("Restore operation must report failure on exception", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)

        // 5. Query all tables directly to verify complete rollback to original snapshot
        val postLoans = targetDb.loanDao().getAllLoansForBackup()
        val postRepayments = targetDb.loanDao().getAllRepaymentsForBackup()
        val postTxs = targetDb.transactionDao().getAllTransactionsForBackup()
        val postSettings = targetDb.budgetSettingDao().getAllSettingsForBackup()
        val postAllocs = targetDb.budgetAllocationDao().getAllAllocationsDirect()

        // Verify counts remain unchanged
        assertEquals(1, postLoans.size)
        assertEquals(1, postRepayments.size)
        assertEquals(1, postTxs.size)
        assertEquals(1, postSettings.size)
        assertEquals(1, postAllocs.size)

        // Verify candidate records were NOT committed
        assertNull(postLoans.find { it.id == 888L })
        assertNull(postRepayments.find { it.id == 999L })
        assertNull(postTxs.find { it.id == 777L })

        // Verify original records survive intact
        assertEquals(initialLoans.first(), postLoans.first())
        assertEquals(initialRepayments.first(), postRepayments.first())
        assertEquals(initialTxs.first(), postTxs.first())
        assertEquals(initialSettings.first(), postSettings.first())
        assertEquals(initialAllocs.first(), postAllocs.first())

        // Verify archive timestamp on transaction remained intact
        assertEquals(archiveTs, postTxs.first().archivedAt)
    }

    // ==========================================
    // TASK 3: Strengthened Validation Tests
    // ==========================================

    @Test
    fun `validation rejects repayment referencing missing loan when loans array is empty`() {
        val json = """
            {
                "backupFormat": "${FinanceBackupData.BACKUP_FORMAT_IDENTIFIER}",
                "backupVersion": 1,
                "loans": [],
                "loanRepayments": [
                    {
                        "id": 1,
                        "loanId": 999,
                        "amount": 500.0,
                        "note": "Orphan repayment",
                        "timestamp": 1700000000000
                    }
                ]
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for repayment referencing non-existent loan ID")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-existent loan ID 999") == true)
        }
    }

    @Test
    fun `validation rejects repayment referencing loan not present in loans list`() {
        val json = """
            {
                "backupFormat": "${FinanceBackupData.BACKUP_FORMAT_IDENTIFIER}",
                "backupVersion": 1,
                "loans": [
                    {
                        "id": 10,
                        "type": "LENT",
                        "personName": "Tareq",
                        "initialAmount": 5000.0,
                        "remainingAmount": 5000.0,
                        "status": "ACTIVE",
                        "startDate": 1700000000000
                    }
                ],
                "loanRepayments": [
                    {
                        "id": 1,
                        "loanId": 99,
                        "amount": 500.0,
                        "note": "Invalid FK",
                        "timestamp": 1700000000000
                    }
                ]
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for missing loan reference")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-existent loan ID 99") == true)
        }
    }

    @Test
    fun `validation rejects transaction referencing non-existent loan ID`() {
        val json = """
            {
                "backupFormat": "${FinanceBackupData.BACKUP_FORMAT_IDENTIFIER}",
                "backupVersion": 1,
                "loans": [],
                "transactions": [
                    {
                        "id": 1,
                        "type": "LOAN",
                        "amount": 1000.0,
                        "categoryId": "loan_lent",
                        "categoryName": "Loan",
                        "note": "",
                        "timestamp": 1700000000000,
                        "loanId": 500
                    }
                ]
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for transaction with invalid loan reference")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("references non-existent loan ID 500") == true)
        }
    }

    @Test
    fun `validation rejects duplicate transaction IDs`() {
        val json = """
            {
                "backupFormat": "${FinanceBackupData.BACKUP_FORMAT_IDENTIFIER}",
                "backupVersion": 1,
                "transactions": [
                    { "id": 5, "type": "INCOME", "amount": 1000.0, "categoryId": "salary", "categoryName": "Salary", "note": "", "timestamp": 1700000000000 },
                    { "id": 5, "type": "EXPENSE", "amount": 200.0, "categoryId": "food", "categoryName": "Food", "note": "", "timestamp": 1700000000000 }
                ]
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for duplicate transaction ID")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Duplicate transaction ID 5") == true)
        }
    }

    @Test
    fun `validation rejects duplicate loan IDs`() {
        val json = """
            {
                "backupFormat": "${FinanceBackupData.BACKUP_FORMAT_IDENTIFIER}",
                "backupVersion": 1,
                "loans": [
                    { "id": 12, "type": "LENT", "personName": "Person A", "initialAmount": 5000.0, "remainingAmount": 5000.0, "status": "ACTIVE", "startDate": 1700000000000 },
                    { "id": 12, "type": "BORROWED", "personName": "Person B", "initialAmount": 3000.0, "remainingAmount": 3000.0, "status": "ACTIVE", "startDate": 1700000000000 }
                ]
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for duplicate loan ID")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Duplicate loan ID 12") == true)
        }
    }

    @Test
    fun `validation rejects unsupported future backup version`() {
        val json = """
            {
                "backupFormat": "${FinanceBackupData.BACKUP_FORMAT_IDENTIFIER}",
                "backupVersion": 99,
                "transactions": []
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for unsupported future backup version")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Unsupported backup version (99)") == true)
        }
    }

    @Test
    fun `validation rejects unrecognized backup format identifier`() {
        val json = """
            {
                "backupFormat": "UNKNOWN_OTHER_APP_BACKUP",
                "backupVersion": 1,
                "transactions": []
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for invalid format")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Unrecognized backup file format") == true)
        }
    }

    @Test
    fun `validation rejects invalid transaction type string`() {
        val json = """
            {
                "backupFormat": "${FinanceBackupData.BACKUP_FORMAT_IDENTIFIER}",
                "backupVersion": 1,
                "transactions": [
                    { "id": 1, "type": "INVALID_TYPE_ENUM", "amount": 1000.0, "categoryId": "other", "categoryName": "Other", "note": "", "timestamp": 1700000000000 }
                ]
            }
        """.trimIndent()

        try {
            FinanceBackupData.fromJsonString(json)
            fail("Expected IllegalArgumentException for invalid transaction type")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid transaction type 'INVALID_TYPE_ENUM'") == true)
        }
    }
}
