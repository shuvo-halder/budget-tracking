package com.engrshuvo.financemanager

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
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.ui.state.MoreSubDestination
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsAndReportingUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        val flow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val archivedFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        fun emit() {
            flow.value = transactions.filter { it.archivedAt == null }.sortedByDescending { it.timestamp }
            archivedFlow.value = transactions.filter { it.archivedAt != null }.sortedByDescending { it.archivedAt ?: 0L }
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flow
        override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = MutableStateFlow(
            transactions.filter { it.type == type && it.archivedAt == null }
        )
        override fun getArchivedTransactions(): Flow<List<TransactionEntity>> = archivedFlow
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
        val archivedLoansFlow = MutableStateFlow<List<LoanEntity>>(emptyList())

        fun emitLoans() {
            flow.value = loans.filter { it.archivedAt == null }.sortedByDescending { it.startDate }
            archivedLoansFlow.value = loans.filter { it.archivedAt != null }
        }

        fun emitRepayments() {
            repaymentsFlow.value = repayments.filter { it.archivedAt == null }
        }

        override fun getAllLoansFlow(): Flow<List<LoanEntity>> = flow
        override fun getActiveLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(
            loans.filter { it.status == LoanStatus.ACTIVE && it.archivedAt == null }
        )
        override fun getLoansByTypeFlow(type: LoanType): Flow<List<LoanEntity>> = MutableStateFlow(
            loans.filter { it.type == type && it.archivedAt == null }
        )
        override fun getLoanByIdFlow(id: Long): Flow<LoanEntity?> = MutableStateFlow(loans.find { it.id == id })
        override fun getArchivedLoans(): Flow<List<LoanEntity>> = archivedLoansFlow
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
        val flows = mutableMapOf<String, MutableStateFlow<BudgetSettingEntity?>>()

        fun emit(key: String) {
            flows.getOrPut(key) { MutableStateFlow(null) }.value = settings[key]
        }

        override fun getSettingFlow(key: String): Flow<BudgetSettingEntity?> =
            flows.getOrPut(key) { MutableStateFlow(settings[key]) }

        override suspend fun getSettingDirect(key: String): BudgetSettingEntity? = settings[key]
        override suspend fun insertOrUpdateSetting(setting: BudgetSettingEntity) {
            settings[setting.settingKey] = setting
            emit(setting.settingKey)
        }

        override suspend fun getAllSettingsForBackup(): List<BudgetSettingEntity> = settings.values.toList()
        override suspend fun clearAllSettings() {
            settings.clear()
            flows.values.forEach { it.value = null }
        }
        override suspend fun insertAllSettings(settings: List<BudgetSettingEntity>) {
            settings.forEach {
                this.settings[it.settingKey] = it
                emit(it.settingKey)
            }
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
    fun `01 - Reports accurately calculates multiple income transactions in month`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val now = System.currentTimeMillis()
        transactionDao.insertTransaction(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 50000.0, categoryId = "salary", categoryName = "Monthly Salary", note = "Primary Job", timestamp = now)
        )
        transactionDao.insertTransaction(
            TransactionEntity(id = 2, type = TransactionType.INCOME, amount = 15000.0, categoryId = "bonus", categoryName = "Bonus", note = "Festival Bonus", timestamp = now)
        )
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.totalIncome == 65000.0 }

        val state = viewModel.uiState.value
        assertEquals(65000.0, state.totalIncome, 0.001)
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(65000.0, state.netOperatingCashChange, 0.001)
    }

    @Test
    fun `02 - Reports accurately calculates expense breakdown and percentages`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val now = System.currentTimeMillis()
        transactionDao.insertTransaction(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 3000.0, categoryId = "food", categoryName = "Food & Dining", note = "Dining out", timestamp = now)
        )
        transactionDao.insertTransaction(
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 2000.0, categoryId = "food", categoryName = "Food & Dining", note = "Snacks", timestamp = now)
        )
        transactionDao.insertTransaction(
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 5000.0, categoryId = "bills", categoryName = "Bills & Utilities", note = "Electricity", timestamp = now)
        )
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.totalExpense == 10000.0 }

        val state = viewModel.uiState.value
        assertEquals(10000.0, state.totalExpense, 0.001)

        val breakdown = state.categorySpendBreakdown
        assertEquals(2, breakdown.size)

        // Food = 5000 (50%), Bills = 5000 (50%)
        val foodItem = breakdown.find { it.category.id == "food" }
        assertNotNull(foodItem)
        assertEquals(5000.0, foodItem!!.totalAmount, 0.001)
        assertEquals(0.5f, foodItem.percentage, 0.01f)
        assertEquals(2, foodItem.transactionCount)

        val billsItem = breakdown.find { it.category.id == "bills" }
        assertNotNull(billsItem)
        assertEquals(5000.0, billsItem!!.totalAmount, 0.001)
        assertEquals(0.5f, billsItem.percentage, 0.01f)
        assertEquals(1, billsItem.transactionCount)
    }

    @Test
    fun `03 - Budget vs Actual properly computes planned vs actual and remaining envelopes`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val monthKey = String.format(Locale.US, "%04d-%02d", year, month)

        budgetAllocationDao.insertOrUpdateAllocations(listOf(
            BudgetAllocationEntity(monthKey = monthKey, categoryId = "food", categoryName = "Food & Dining", allocatedAmount = 8000.0),
            BudgetAllocationEntity(monthKey = monthKey, categoryId = "transport", categoryName = "Transportation", allocatedAmount = 4000.0)
        ))
        transactionDao.insertTransaction(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 6000.0, categoryId = "food", categoryName = "Food & Dining", note = "Groceries", timestamp = now)
        )
        transactionDao.insertTransaction(
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 5000.0, categoryId = "transport", categoryName = "Transportation", note = "Fuel", timestamp = now)
        )
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.monthAllocations.isNotEmpty() }

        val allocations = viewModel.uiState.value.monthAllocations
        val foodAlloc = allocations.find { it.category.id == "food" }
        assertNotNull(foodAlloc)
        assertEquals(8000.0, foodAlloc!!.allocatedAmount, 0.001)
        assertEquals(6000.0, foodAlloc.actualSpent, 0.001)
        assertEquals(2000.0, foodAlloc.remainingAmount, 0.001)
        assertEquals(0.75f, foodAlloc.usagePercentage, 0.01f)
        assertFalse(foodAlloc.isOverBudget)

        val transportAlloc = allocations.find { it.category.id == "transport" }
        assertNotNull(transportAlloc)
        assertEquals(4000.0, transportAlloc!!.allocatedAmount, 0.001)
        assertEquals(5000.0, transportAlloc.actualSpent, 0.001)
        assertEquals(-1000.0, transportAlloc.remainingAmount, 0.001)
        assertEquals(1.25f, transportAlloc.usagePercentage, 0.01f)
        assertTrue(transportAlloc.isOverBudget)
    }

    @Test
    fun `04 - Invariant Check - Planned savings is NOT counted as expense`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val monthKey = String.format(Locale.US, "%04d-%02d", year, month)

        transactionDao.insertTransaction(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 50000.0, categoryId = "salary", categoryName = "Monthly Salary", note = "Salary", timestamp = now)
        )
        budgetAllocationDao.insertOrUpdateAllocations(listOf(
            BudgetAllocationEntity(monthKey = monthKey, categoryId = "savings", categoryName = "Savings & Investment", allocatedAmount = 15000.0),
            BudgetAllocationEntity(monthKey = monthKey, categoryId = "emergency", categoryName = "Emergency Fund", allocatedAmount = 5000.0)
        ))
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.plannedSavingsTotal == 20000.0 }

        val state = viewModel.uiState.value
        assertEquals(20000.0, state.plannedSavingsTotal, 0.001)
        // Expense MUST be 0 since no expense transactions were recorded
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(50000.0, state.netOperatingCashChange, 0.001)
    }

    @Test
    fun `05 - Invariant Check - Loan principal and repayments are NOT counted as ordinary expense`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val now = System.currentTimeMillis()
        loanDao.insertLoan(
            LoanEntity(id = 1, type = LoanType.LENT, personName = "Hasan", phoneNumber = "01700000000", initialAmount = 10000.0, remainingAmount = 10000.0, status = LoanStatus.ACTIVE, startDate = now, note = "Lent money")
        )
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.allLoans.isNotEmpty() }

        val state = viewModel.uiState.value
        assertEquals(10000.0, state.totalActiveLent, 0.001)
        assertEquals(0.0, state.totalActiveBorrowed, 0.001)
        // Ordinary totalExpense MUST be 0
        assertEquals(0.0, state.totalExpense, 0.001)
    }

    @Test
    fun `06 - Invariant Check - Archived transactions are excluded from active reports`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val now = System.currentTimeMillis()
        transactionDao.insertTransaction(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 5000.0, categoryId = "food", categoryName = "Food & Dining", note = "Archived food", timestamp = now)
        )
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.totalExpense == 5000.0 }

        transactionDao.archiveTransaction(1, System.currentTimeMillis())
        advanceUntilIdle()

        waitUntil { viewModel.uiState.value.totalExpense == 0.0 }

        val state = viewModel.uiState.value
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(0, state.categorySpendBreakdown.size)
    }

    @Test
    fun `07 - Reports navigation to MoreSubDestination REPORTS and month changes`() = testScope.runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(MoreSubDestination.NONE, viewModel.uiState.value.moreSubDestination)
        viewModel.navigateToMoreSubDestination(MoreSubDestination.REPORTS)
        advanceUntilIdle()
        waitUntil { viewModel.uiState.value.moreSubDestination == MoreSubDestination.REPORTS }
        assertEquals(MoreSubDestination.REPORTS, viewModel.uiState.value.moreSubDestination)

        val curMonth = viewModel.uiState.value.displayedMonth.get(Calendar.MONTH)
        viewModel.changeMonth(-1)
        advanceUntilIdle()
        waitUntil { viewModel.uiState.value.displayedMonth.get(Calendar.MONTH) != curMonth }
        val prevMonth = viewModel.uiState.value.displayedMonth.get(Calendar.MONTH)
        assertEquals((curMonth - 1 + 12) % 12, prevMonth)

        viewModel.resetToCurrentMonth()
        advanceUntilIdle()
        waitUntil { viewModel.uiState.value.displayedMonth.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) }
        val resetMonth = viewModel.uiState.value.displayedMonth.get(Calendar.MONTH)
        assertEquals(Calendar.getInstance().get(Calendar.MONTH), resetMonth)
    }
}
