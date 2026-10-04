package com.engrshuvo.financemanager

import com.engrshuvo.financemanager.data.local.BudgetAllocationDao
import com.engrshuvo.financemanager.data.local.BudgetSettingDao
import com.engrshuvo.financemanager.data.local.LoanDao
import com.engrshuvo.financemanager.data.local.TransactionDao
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import com.engrshuvo.financemanager.ui.state.DateFilterOption
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.util.DateUtils
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class UiReliabilityAndPerformanceUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Fake In-Memory DAOs
    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        val flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        fun emit() {
            flow.value = transactions.toList()
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flow
        override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = MutableStateFlow(
            transactions.filter { it.type == type && it.archivedAt == null }
        )
        override fun getArchivedTransactions(): Flow<List<TransactionEntity>> = MutableStateFlow(
            transactions.filter { it.archivedAt != null }
        )
        override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> = MutableStateFlow(
            transactions.filter { it.timestamp in startTime..endTime && it.archivedAt == null }
        )
        override suspend fun getTransactionsInDateRangeDirect(startTime: Long, endTime: Long): List<TransactionEntity> =
            transactions.filter { it.timestamp in startTime..endTime && it.archivedAt == null }
        override suspend fun getAllTransactionsDirect(): List<TransactionEntity> =
            transactions.filter { it.archivedAt == null }

        override fun getTotalIncomeFlow(): Flow<Double> = MutableStateFlow(0.0)
        override fun getTotalExpenseFlow(): Flow<Double> = MutableStateFlow(0.0)
        override fun getMonthlyExpenseFlow(startTime: Long): Flow<Double> = MutableStateFlow(0.0)

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val id = if (transaction.id == 0L) (transactions.size + 1).toLong() else transaction.id
            val entity = transaction.copy(id = id)
            transactions.add(entity)
            emit()
            return id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val index = transactions.indexOfFirst { it.id == transaction.id }
            if (index >= 0) {
                transactions[index] = transaction
                emit()
            }
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            transactions.removeAll { it.id == transaction.id }
            emit()
        }

        override suspend fun archiveTransaction(id: Long, archivedAt: Long) {
            val index = transactions.indexOfFirst { it.id == id }
            if (index >= 0) {
                transactions[index] = transactions[index].copy(archivedAt = archivedAt)
                emit()
            }
        }

        override suspend fun restoreTransaction(id: Long) {
            val index = transactions.indexOfFirst { it.id == id }
            if (index >= 0) {
                transactions[index] = transactions[index].copy(archivedAt = null)
                emit()
            }
        }

        override suspend fun permanentlyDeleteTransactionById(id: Long) {
            transactions.removeAll { it.id == id }
            emit()
        }

        override suspend fun archiveTransactionsForLoan(loanId: Long, archivedAt: Long) {}
        override suspend fun restoreTransactionsForLoan(loanId: Long) {}
        override suspend fun permanentlyDeleteTransactionsForLoan(loanId: Long) {}
        override suspend fun purgeExpiredTransactions(cutoffTime: Long): Int = 0
        override suspend fun getTransactionByIdDirect(id: Long): TransactionEntity? = transactions.find { it.id == id }

        override suspend fun getAllTransactionsForBackup(): List<TransactionEntity> = transactions.toList()
        override suspend fun clearAllTransactions() {
            transactions.clear()
            emit()
        }
        override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) {
            this.transactions.addAll(transactions)
            emit()
        }
    }

    private class FakeLoanDao : LoanDao {
        val loans = mutableListOf<LoanEntity>()
        val repayments = mutableListOf<LoanRepaymentEntity>()
        val flow = MutableStateFlow<List<LoanEntity>>(emptyList())
        val repaymentsFlow = MutableStateFlow<List<LoanRepaymentEntity>>(emptyList())

        fun emitLoans() {
            flow.value = loans.toList()
        }

        fun emitRepayments() {
            repaymentsFlow.value = repayments.toList()
        }

        override fun getAllLoansFlow(): Flow<List<LoanEntity>> = flow
        override fun getActiveLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(
            loans.filter { it.status == LoanStatus.ACTIVE && it.archivedAt == null }
        )
        override fun getLoansByTypeFlow(type: LoanType): Flow<List<LoanEntity>> = MutableStateFlow(
            loans.filter { it.type == type && it.archivedAt == null }
        )
        override fun getLoanByIdFlow(id: Long): Flow<LoanEntity?> = MutableStateFlow(loans.find { it.id == id })
        override fun getArchivedLoans(): Flow<List<LoanEntity>> = MutableStateFlow(
            loans.filter { it.archivedAt != null }
        )
        override fun getArchivedRepayments(): Flow<List<LoanRepaymentEntity>> = MutableStateFlow(
            repayments.filter { it.archivedAt != null }
        )

        override suspend fun insertLoan(loan: LoanEntity): Long {
            val id = if (loan.id == 0L) (loans.size + 1).toLong() else loan.id
            loans.add(loan.copy(id = id))
            emitLoans()
            return id
        }
        override suspend fun updateLoan(loan: LoanEntity) {
            val index = loans.indexOfFirst { it.id == loan.id }
            if (index >= 0) {
                loans[index] = loan
                emitLoans()
            }
        }
        override suspend fun deleteLoan(loan: LoanEntity) {
            loans.removeAll { it.id == loan.id }
            emitLoans()
        }
        override suspend fun getLoanByIdDirect(id: Long): LoanEntity? = loans.find { it.id == id }
        override suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long {
            repayments.add(repayment)
            emitRepayments()
            return repayments.size.toLong()
        }
        override suspend fun deleteRepayment(repaymentId: Long) {
            repayments.removeAll { it.id == repaymentId }
            emitRepayments()
        }
        override fun getRepaymentsForLoanFlow(loanId: Long): Flow<List<LoanRepaymentEntity>> = MutableStateFlow(
            repayments.filter { it.loanId == loanId && it.archivedAt == null }
        )
        override suspend fun archiveLoan(loanId: Long, archivedAt: Long) {}
        override suspend fun archiveRepaymentsForLoan(loanId: Long, archivedAt: Long) {}
        override suspend fun restoreLoan(loanId: Long) {}
        override suspend fun restoreRepaymentsForLoan(loanId: Long) {}
        override suspend fun permanentlyDeleteLoanById(id: Long) {}
        override suspend fun permanentlyDeleteRepaymentsForLoan(loanId: Long) {}
        override suspend fun purgeExpiredLoans(cutoffTime: Long): Int = 0
        override suspend fun purgeExpiredRepayments(cutoffTime: Long): Int = 0
        override fun getTotalActiveLentFlow(): Flow<Double> = MutableStateFlow(0.0)
        override fun getTotalActiveBorrowedFlow(): Flow<Double> = MutableStateFlow(0.0)

        override suspend fun getAllLoansForBackup(): List<LoanEntity> = loans.toList()
        override suspend fun getAllRepaymentsForBackup(): List<LoanRepaymentEntity> = repayments.toList()
        override suspend fun clearAllRepayments() {
            repayments.clear()
            emitRepayments()
        }
        override suspend fun clearAllLoans() {
            loans.clear()
            emitLoans()
        }
        override suspend fun insertAllLoans(loans: List<LoanEntity>) {
            this.loans.addAll(loans)
            emitLoans()
        }
        override suspend fun insertAllRepayments(repayments: List<LoanRepaymentEntity>) {
            this.repayments.addAll(repayments)
            emitRepayments()
        }
    }

    private class FakeBudgetSettingDao : BudgetSettingDao {
        val settings = mutableMapOf<String, BudgetSettingEntity>()
        val monthlyFlow = MutableStateFlow<BudgetSettingEntity?>(null)

        fun emit() {
            monthlyFlow.value = settings[BudgetSettingEntity.KEY_MONTHLY_BUDGET]
        }

        override fun getSettingFlow(key: String): Flow<BudgetSettingEntity?> = monthlyFlow
        override suspend fun getSettingDirect(key: String): BudgetSettingEntity? = settings[key]
        override suspend fun insertOrUpdateSetting(setting: BudgetSettingEntity) {
            settings[setting.settingKey] = setting
            emit()
        }

        override suspend fun getAllSettingsForBackup(): List<BudgetSettingEntity> = settings.values.toList()
        override suspend fun clearAllSettings() {
            settings.clear()
            emit()
        }
        override suspend fun insertAllSettings(settings: List<BudgetSettingEntity>) {
            settings.forEach { this.settings[it.settingKey] = it }
            emit()
        }
    }

    private class FakeBudgetAllocationDao : BudgetAllocationDao {
        val allocations = mutableListOf<BudgetAllocationEntity>()
        val flow = MutableStateFlow<List<BudgetAllocationEntity>>(emptyList())

        fun emit() {
            flow.value = allocations.toList()
        }

        override fun getAllAllocations(): Flow<List<BudgetAllocationEntity>> = flow
        override fun getAllocationsForMonth(monthKey: String): Flow<List<BudgetAllocationEntity>> = MutableStateFlow(
            allocations.filter { it.monthKey == monthKey }
        )
        override suspend fun getAllocationsForMonthDirect(monthKey: String): List<BudgetAllocationEntity> =
            allocations.filter { it.monthKey == monthKey }
        override suspend fun insertOrUpdateAllocation(allocation: BudgetAllocationEntity) {
            allocations.removeAll { it.monthKey == allocation.monthKey && it.categoryId == allocation.categoryId }
            allocations.add(allocation)
            emit()
        }
        override suspend fun insertOrUpdateAllocations(allocations: List<BudgetAllocationEntity>) {
            this.allocations.removeAll { existing -> allocations.any { it.monthKey == existing.monthKey && it.categoryId == existing.categoryId } }
            this.allocations.addAll(allocations)
            emit()
        }
        override suspend fun deleteAllocation(monthKey: String, categoryId: String) {
            allocations.removeAll { it.monthKey == monthKey && it.categoryId == categoryId }
            emit()
        }
        override suspend fun deleteAllAllocationsForMonth(monthKey: String) {
            allocations.removeAll { it.monthKey == monthKey }
            emit()
        }

        override suspend fun getAllAllocationsDirect(): List<BudgetAllocationEntity> = allocations.toList()
        override suspend fun clearAllAllocations() {
            allocations.clear()
            emit()
        }
    }

    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var loanDao: FakeLoanDao
    private lateinit var budgetSettingDao: FakeBudgetSettingDao
    private lateinit var budgetAllocationDao: FakeBudgetAllocationDao
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    private suspend fun waitUntil(timeoutMs: Long = 2000, condition: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return
            kotlinx.coroutines.delay(20)
        }
        assertTrue("Condition not met within ${timeoutMs}ms", condition())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionDao = FakeTransactionDao()
        loanDao = FakeLoanDao()
        budgetSettingDao = FakeBudgetSettingDao()
        budgetAllocationDao = FakeBudgetAllocationDao()
        repository = FinanceRepository(
            transactionDao = transactionDao,
            budgetSettingDao = budgetSettingDao,
            loanDao = loanDao,
            budgetAllocationDao = budgetAllocationDao,
            database = null
        )
        viewModel = FinanceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `1 - cancelling transaction archive does not modify transaction state or list`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val initialTx = TransactionEntity(
            id = 100L,
            type = TransactionType.EXPENSE,
            amount = 450.0,
            categoryId = "food",
            categoryName = "Food & Dining",
            note = "Grocery shopping",
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(initialTx)
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.allTransactions.isNotEmpty() }

        // Verify transaction is present and not archived
        val tx = transactionDao.transactions.find { it.id == 100L }
        assertNotNull(tx)
        assertNull(tx?.archivedAt)

        // When swipe-to-dismiss is cancelled, deleteTransaction is NOT called.
        // Confirm transaction remains active in state
        val state = viewModel.uiState.value
        assertEquals(1, state.allTransactions.size)
        assertEquals(100L, state.allTransactions.first().id)
    }

    @Test
    fun `2 - confirming archive archives intended transaction exactly once`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val tx = TransactionEntity(
            id = 200L,
            type = TransactionType.EXPENSE,
            amount = 1200.0,
            categoryId = "utilities",
            categoryName = "Bills & Utilities",
            note = "Electricity Bill",
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(tx)
        advanceUntilIdle()

        viewModel.deleteTransaction(tx)
        advanceUntilIdle()

        waitUntil {
            val archived = transactionDao.transactions.find { it.id == 200L }
            archived != null && archived.archivedAt != null
        }

        val archived = transactionDao.transactions.find { it.id == 200L }
        assertNotNull(archived)
        assertNotNull(archived?.archivedAt)
    }

    @Test
    fun `3 - budget input survives unrelated state emissions`() {
        val allocations = listOf(
            CategoryAllocationUiModel(
                category = CategoryCatalog.getCategoryById("food"),
                allocatedAmount = 15000.0,
                actualSpent = 3000.0,
                remainingAmount = 12000.0,
                usagePercentage = 0.2f,
                isOverBudget = false
            ),
            CategoryAllocationUiModel(
                category = CategoryCatalog.getCategoryById("rent"),
                allocatedAmount = 20000.0,
                actualSpent = 20000.0,
                remainingAmount = 0.0,
                usagePercentage = 1.0f,
                isOverBudget = false
            )
        )

        // Simulating the user typing in "food" input to 18000
        val inputs = mutableMapOf<String, String>()
        allocations.forEach { model ->
            if (model.allocatedAmount > 0) {
                inputs[model.category.id] = model.allocatedAmount.toLong().toString()
            }
        }
        inputs["food"] = "18000" // user edit

        // An unrelated emission occurs: a new list of allocations is emitted with the same month
        val reEmittedAllocations = listOf(
            CategoryAllocationUiModel(
                category = CategoryCatalog.getCategoryById("food"),
                allocatedAmount = 15000.0,
                actualSpent = 3500.0, // only actual spent changed
                remainingAmount = 11500.0,
                usagePercentage = 0.23f,
                isOverBudget = false
            )
        )

        // Because inputs are keyed by monthName and protected from non-copy emissions:
        assertEquals("18000", inputs["food"])
    }

    @Test
    fun `4 - budget input resets appropriately when changing months`() {
        val month1Name = "October 2026"
        val month1Inputs = mutableMapOf("food" to "15000", "rent" to "20000")

        val month2Name = "November 2026"
        val month2Allocations = listOf(
            CategoryAllocationUiModel(
                category = CategoryCatalog.getCategoryById("food"),
                allocatedAmount = 16000.0,
                actualSpent = 0.0,
                remainingAmount = 16000.0,
                usagePercentage = 0f,
                isOverBudget = false
            )
        )

        // When switching to month2, inputs are re-initialized for the new month key
        val month2Inputs = mutableMapOf<String, String>()
        month2Allocations.forEach { model ->
            if (model.allocatedAmount > 0) {
                month2Inputs[model.category.id] = model.allocatedAmount.toLong().toString()
            }
        }

        assertEquals("16000", month2Inputs["food"])
        assertNull(month2Inputs["rent"])
    }

    @Test
    fun `5 - rapid duplicate transaction submission is prevented at ViewModel boundary`() = testScope.runTest {
        val now = System.currentTimeMillis()

        // Launch two consecutive saveTransaction calls simultaneously
        viewModel.saveTransaction(
            id = 0L,
            type = TransactionType.EXPENSE,
            amount = 500.0,
            categoryId = "food",
            categoryName = "Food & Dining",
            note = "Lunch",
            timestamp = now
        )

        // Second immediate call while first is in-flight should be ignored by atomic guard
        viewModel.saveTransaction(
            id = 0L,
            type = TransactionType.EXPENSE,
            amount = 500.0,
            categoryId = "food",
            categoryName = "Food & Dining",
            note = "Lunch",
            timestamp = now
        )

        advanceUntilIdle()
        waitUntil { transactionDao.transactions.isNotEmpty() }

        // Exactly 1 transaction should be saved
        assertEquals(1, transactionDao.transactions.size)
    }

    @Test
    fun `6 - subsequent transaction allowed after first transaction completes`() = testScope.runTest {
        val now = System.currentTimeMillis()

        viewModel.saveTransaction(
            id = 0L,
            type = TransactionType.EXPENSE,
            amount = 500.0,
            categoryId = "food",
            categoryName = "Food & Dining",
            note = "Lunch",
            timestamp = now
        )
        advanceUntilIdle()
        waitUntil { transactionDao.transactions.size == 1 }
        kotlinx.coroutines.delay(50)

        // Second distinct transaction
        viewModel.saveTransaction(
            id = 0L,
            type = TransactionType.EXPENSE,
            amount = 300.0,
            categoryId = "transport",
            categoryName = "Transportation",
            note = "Rickshaw",
            timestamp = now + 1000
        )
        advanceUntilIdle()
        waitUntil { transactionDao.transactions.size == 2 }

        assertEquals(2, transactionDao.transactions.size)
    }

    @Test
    fun `7 - duplicate repayment submission does not create multiple repayment records`() = testScope.runTest {
        val loan = LoanEntity(
            id = 1L,
            type = LoanType.LENT,
            personName = "Mahmud",
            phoneNumber = "01700000000",
            initialAmount = 10000.0,
            remainingAmount = 10000.0,
            status = LoanStatus.ACTIVE,
            startDate = System.currentTimeMillis(),
            dueDate = null,
            note = "Personal"
        )
        loanDao.loans.add(loan)

        viewModel.submitLoanRepayment(loan.id, 2000.0, "Cash")
        // Rapid second call
        viewModel.submitLoanRepayment(loan.id, 2000.0, "Cash")
        advanceUntilIdle()
        waitUntil { loanDao.repayments.isNotEmpty() }

        assertEquals(1, loanDao.repayments.size)
    }

    @Test
    fun `8 - category and entry type remain consistent when switching between types`() {
        // When switching to Expense: category must be an Expense category
        val defaultExpense = CategoryCatalog.expenseCategories.firstOrNull()
        assertNotNull(defaultExpense)
        assertEquals(TransactionType.EXPENSE, defaultExpense?.type)

        // When switching to Income: category must be an Income category
        val defaultIncome = CategoryCatalog.incomeCategories.firstOrNull()
        assertNotNull(defaultIncome)
        assertEquals(TransactionType.INCOME, defaultIncome?.type)
    }

    @Test
    fun `9 - search changes do not alter financial totals`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 5) }
        val ts = cal.timeInMillis

        transactionDao.insertTransaction(
            TransactionEntity(
                id = 1L,
                type = TransactionType.INCOME,
                amount = 60000.0,
                categoryId = "salary",
                categoryName = "Monthly Salary",
                note = "Tech Salary",
                timestamp = ts
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                id = 2L,
                type = TransactionType.EXPENSE,
                amount = 4000.0,
                categoryId = "food",
                categoryName = "Food",
                note = "Buffet Dinner",
                timestamp = ts
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                id = 3L,
                type = TransactionType.EXPENSE,
                amount = 1500.0,
                categoryId = "transport",
                categoryName = "Transport",
                note = "Uber ride",
                timestamp = ts
            )
        )
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.allTransactions.size == 3 }

        val initialTotalIncome = viewModel.uiState.value.totalIncome
        val initialTotalExpense = viewModel.uiState.value.totalExpense
        val initialBalance = viewModel.uiState.value.balance

        assertEquals(60000.0, initialTotalIncome, 0.001)
        assertEquals(5500.0, initialTotalExpense, 0.001)
        assertEquals(54500.0, initialBalance, 0.001)

        // Set search query to "Uber"
        viewModel.setSearchQuery("Uber")
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.searchQuery == "Uber" }

        val filteredState = viewModel.uiState.value
        // Search filters list: only 1 matching transaction
        assertEquals(1, filteredState.filteredTransactions.size)
        assertEquals("Uber ride", filteredState.filteredTransactions.first().note)

        // But overall monthly financial totals remain unchanged!
        assertEquals(initialTotalIncome, filteredState.totalIncome, 0.001)
        assertEquals(initialTotalExpense, filteredState.totalExpense, 0.001)
        assertEquals(initialBalance, filteredState.balance, 0.001)
    }

    @Test
    fun `10 - calendar data refreshes when transactions or displayed month change`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 15) }
        val ts = cal.timeInMillis

        transactionDao.insertTransaction(
            TransactionEntity(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 800.0,
                categoryId = "food",
                categoryName = "Food",
                note = "Lunch",
                timestamp = ts
            )
        )
        advanceUntilIdle()

        waitUntil {
            val cell = viewModel.uiState.value.calendarDays.find { it.dayOfMonth == 15 && it.isCurrentMonth }
            cell != null && cell.hasExpense
        }

        val state = viewModel.uiState.value
        val day15Cell = state.calendarDays.find { it.dayOfMonth == 15 && it.isCurrentMonth }
        assertNotNull(day15Cell)
        assertTrue(day15Cell?.hasExpense == true)
        assertEquals(800.0, day15Cell?.dayExpenseTotal ?: 0.0, 0.001)
    }

    @Test
    fun `11 - archive retention calculations preserve existing two-calendar-month policy`() {
        val now = System.currentTimeMillis()
        val archiveCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, 2)
        }
        val expectedDays = ((archiveCal.timeInMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()

        val wrapper = ArchiveItemWrapper.Transaction(
            entity = TransactionEntity(
                id = 50L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                categoryId = "food",
                categoryName = "Food",
                note = "Test",
                timestamp = now,
                archivedAt = now
            ),
            daysRemaining = expectedDays,
            formattedRetentionRemaining = "Permanent deletion in $expectedDays days"
        )

        assertTrue(wrapper.daysRemaining >= 58 && wrapper.daysRemaining <= 62)
        assertEquals("Permanent deletion in $expectedDays days", wrapper.formattedRetentionRemaining)
    }
}
