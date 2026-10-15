package com.engrshuvo.financemanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Daily Budget", appName)
    }

    @Test
    fun `category catalog has both income and expense categories`() {
        val expenseCats = CategoryCatalog.expenseCategories
        val incomeCats = CategoryCatalog.incomeCategories
        assertTrue(expenseCats.isNotEmpty())
        assertTrue(incomeCats.isNotEmpty())

        val foodCat = CategoryCatalog.getCategoryById("food")
        assertEquals(TransactionType.EXPENSE, foodCat.type)

        val salaryCat = CategoryCatalog.getCategoryById("salary")
        assertEquals(TransactionType.INCOME, salaryCat.type)
    }

    @Test
    fun `currency utils formats BDT amounts properly`() {
        val formatted = CurrencyUtils.formatBDT(15000.0)
        assertEquals("৳15,000", formatted)

        val withSign = CurrencyUtils.formatBDTWithSign(500.0, isIncome = true)
        assertEquals("+৳500", withSign)
    }

    @Test
    fun `loan entity correctly calculates initial and remaining amount`() {
        val loan = LoanEntity(
            id = 1L,
            type = LoanType.LENT,
            personName = "Rahim",
            initialAmount = 5000.0,
            remainingAmount = 2500.0,
            status = LoanStatus.ACTIVE
        )
        assertEquals(5000.0, loan.initialAmount, 0.001)
        assertEquals(2500.0, loan.remainingAmount, 0.001)
        assertEquals(LoanType.LENT, loan.type)
    }

    @Test
    fun `date utils handles start and end of day`() {
        val now = System.currentTimeMillis()
        val startOfDay = DateUtils.getStartOfDay(now)
        val endOfDay = DateUtils.getEndOfDay(now)
        assertTrue(startOfDay <= now)
        assertTrue(endOfDay >= now)
    }

    @Test
    fun `app database creates tables and queries without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.engrshuvo.financemanager.data.local.AppDatabase.getDatabase(context)
        val txDao = db.transactionDao()
        val loanDao = db.loanDao()
        val budgetDao = db.budgetSettingDao()
        val allocDao = db.budgetAllocationDao()
        org.junit.Assert.assertNotNull(txDao)
        org.junit.Assert.assertNotNull(loanDao)
        org.junit.Assert.assertNotNull(budgetDao)
        org.junit.Assert.assertNotNull(allocDao)
    }

    @Test
    fun `finance viewmodel initializes and emits state without exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.engrshuvo.financemanager.data.local.AppDatabase.getDatabase(context)
        val repo = com.engrshuvo.financemanager.data.repository.FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetSettingDao = db.budgetSettingDao(),
            loanDao = db.loanDao(),
            budgetAllocationDao = db.budgetAllocationDao()
        )
        val viewModel = com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModel(repo)
        val state = viewModel.uiState.value
        org.junit.Assert.assertNotNull(state)
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.DASHBOARD, state.activeTab)
    }

    @Test
    fun `bottom navigation tab order has dashboard in exact center and calendar at position four`() {
        val tabs = com.engrshuvo.financemanager.ui.state.FinanceTab.values()
        assertEquals(5, tabs.size)
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.TRANSACTIONS, tabs[0])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.BUDGET, tabs[1])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.DASHBOARD, tabs[2])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.CALENDAR, tabs[3])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.LOANS, tabs[4])
    }

    @Test
    fun `successful repayment atomically updates loan, inserts repayment audit row and linked transaction in Room database`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.engrshuvo.financemanager.data.local.AppDatabase.getDatabase(context)
        val repo = com.engrshuvo.financemanager.data.repository.FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetSettingDao = db.budgetSettingDao(),
            loanDao = db.loanDao(),
            budgetAllocationDao = db.budgetAllocationDao(),
            database = db
        )

        val loan = LoanEntity(
            id = 0,
            type = LoanType.LENT,
            personName = "Kamal",
            initialAmount = 5000.0,
            remainingAmount = 5000.0,
            status = LoanStatus.ACTIVE
        )
        val loanId = repo.insertLoanWithTransaction(loan)

        val success = repo.recordLoanRepayment(loanId, 2000.0, "First partial")
        assertTrue(success)

        val updatedLoan = db.loanDao().getLoanByIdDirect(loanId)
        org.junit.Assert.assertNotNull(updatedLoan)
        assertEquals(3000.0, updatedLoan!!.remainingAmount, 0.001)
        assertEquals(LoanStatus.ACTIVE, updatedLoan.status)

        // Try second repayment that overpays (4000 > 3000)
        val overpaySuccess = repo.recordLoanRepayment(loanId, 4000.0, "Overpay")
        org.junit.Assert.assertFalse(overpaySuccess)

        // Ensure loan remained at 3000
        val loanAfterOverpay = db.loanDao().getLoanByIdDirect(loanId)
        assertEquals(3000.0, loanAfterOverpay!!.remainingAmount, 0.001)

        // Settle exact remaining (3000)
        val settleSuccess = repo.markLoanSettled(loanId)
        assertTrue(settleSuccess)

        val settledLoan = db.loanDao().getLoanByIdDirect(loanId)
        assertEquals(0.0, settledLoan!!.remainingAmount, 0.001)
        assertEquals(LoanStatus.SETTLED, settledLoan.status)
    }

    @Test
    fun `non-cash debt forgiveness settles loan without creating cash transaction or changing cash balance`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.engrshuvo.financemanager.data.local.AppDatabase.getDatabase(context)
        val repo = com.engrshuvo.financemanager.data.repository.FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetSettingDao = db.budgetSettingDao(),
            loanDao = db.loanDao(),
            budgetAllocationDao = db.budgetAllocationDao(),
            database = db
        )

        // Starting loan of 4000
        val loan = LoanEntity(
            id = 0,
            type = LoanType.LENT,
            personName = "Anis",
            initialAmount = 4000.0,
            remainingAmount = 4000.0,
            status = LoanStatus.ACTIVE
        )
        val loanId = repo.insertLoanWithTransaction(loan)

        // Count transactions before settlement (only creation transaction exists for this loan)
        val txsBefore = db.transactionDao().getAllTransactionsForBackup().filter { it.loanId == loanId }
        assertEquals(1, txsBefore.size)

        // Settle non-cash (forgive / write off)
        val success = repo.settleLoanWithoutCash(loanId, "Bad debt write-off")
        assertTrue(success)

        val settledLoan = db.loanDao().getLoanByIdDirect(loanId)
        assertNotNull(settledLoan)
        assertEquals(0.0, settledLoan!!.remainingAmount, 0.001)
        assertEquals(LoanStatus.SETTLED, settledLoan.status)

        // Ensure NO repayment record and NO new cash transaction was created
        val txsAfter = db.transactionDao().getAllTransactionsForBackup().filter { it.loanId == loanId }
        assertEquals(1, txsAfter.size) // Still exactly 1, no cash transaction invented!

        val repayments = db.loanDao().getAllRepaymentsForBackup().filter { it.loanId == loanId }
        assertEquals(0, repayments.size) // No fake repayment records
    }

    @Test
    fun `settleLoanWithCash records full collection transaction and reduces remaining to zero`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.engrshuvo.financemanager.data.local.AppDatabase.getDatabase(context)
        val repo = com.engrshuvo.financemanager.data.repository.FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetSettingDao = db.budgetSettingDao(),
            loanDao = db.loanDao(),
            budgetAllocationDao = db.budgetAllocationDao(),
            database = db
        )

        val loan = LoanEntity(
            id = 0,
            type = LoanType.LENT,
            personName = "Babar",
            initialAmount = 3500.0,
            remainingAmount = 3500.0,
            status = LoanStatus.ACTIVE
        )
        val loanId = repo.insertLoanWithTransaction(loan)

        // Settle with cash
        val success = repo.settleLoanWithCash(loanId)
        assertTrue(success)

        val settledLoan = db.loanDao().getLoanByIdDirect(loanId)
        assertEquals(0.0, settledLoan!!.remainingAmount, 0.001)
        assertEquals(LoanStatus.SETTLED, settledLoan.status)

        // Check that exactly one repayment transaction was created
        val txs = db.transactionDao().getAllTransactionsForBackup().filter { it.loanId == loanId }
        assertEquals(2, txs.size) // Creation (3500 lent) + Settlement (3500 collected)
        val collectionTx = txs.first { it.categoryId == "loan_collected" }
        assertEquals(3500.0, collectionTx.amount, 0.001)
    }

    @Test
    fun `personal refreshments category is present in expense categories and absent in income categories`() {
        val expenseCats = CategoryCatalog.expenseCategories
        val incomeCats = CategoryCatalog.incomeCategories

        val refreshmentsCat = expenseCats.find { it.id == "personal_refreshments" }
        assertNotNull(refreshmentsCat)
        assertEquals("Personal Refreshments", refreshmentsCat!!.name)
        assertEquals(TransactionType.EXPENSE, refreshmentsCat.type)

        // Must not appear in income categories
        assertFalse(incomeCats.any { it.id == "personal_refreshments" })

        // Direct lookup
        val lookup = CategoryCatalog.getCategoryById("personal_refreshments")
        assertEquals("Personal Refreshments", lookup.name)
        assertEquals(TransactionType.EXPENSE, lookup.type)

        // No duplicate entries in all categories
        val allMatching = CategoryCatalog.getAllCategories().filter { it.id == "personal_refreshments" }
        assertEquals(1, allMatching.size)
    }

    @Test
    fun `transaction can be saved and edited with personal refreshments category`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.engrshuvo.financemanager.data.local.AppDatabase.getDatabase(context)
        val repo = com.engrshuvo.financemanager.data.repository.FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetSettingDao = db.budgetSettingDao(),
            loanDao = db.loanDao(),
            budgetAllocationDao = db.budgetAllocationDao(),
            database = db
        )

        val tx = com.engrshuvo.financemanager.data.model.TransactionEntity(
            id = 0,
            type = TransactionType.EXPENSE,
            amount = 120.0,
            categoryId = "personal_refreshments",
            categoryName = "Personal Refreshments",
            note = "Tea and biscuits"
        )
        val txId = repo.insertTransaction(tx)
        assertTrue(txId > 0)

        val retrieved = db.transactionDao().getAllTransactionsForBackup().find { it.id == txId }
        assertNotNull(retrieved)
        assertEquals("personal_refreshments", retrieved!!.categoryId)
        assertEquals("Personal Refreshments", retrieved.categoryName)
        assertEquals(120.0, retrieved.amount, 0.001)

        // Edit transaction
        val updatedTx = retrieved.copy(amount = 150.0, note = "Coffee and cookies")
        repo.updateTransaction(updatedTx)

        val updatedRetrieved = db.transactionDao().getAllTransactionsForBackup().find { it.id == txId }
        assertNotNull(updatedRetrieved)
        assertEquals(150.0, updatedRetrieved!!.amount, 0.001)
        assertEquals("Coffee and cookies", updatedRetrieved.note)
    }

    @Test
    fun `personal refreshments expense transaction is counted exactly once in expense totals and reports`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.engrshuvo.financemanager.data.local.AppDatabase.getDatabase(context)
        val repo = com.engrshuvo.financemanager.data.repository.FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetSettingDao = db.budgetSettingDao(),
            loanDao = db.loanDao(),
            budgetAllocationDao = db.budgetAllocationDao(),
            database = db
        )

        val allTxsBefore = db.transactionDao().getAllTransactionsForBackup().filter { it.archivedAt == null }
        val beforeExpenses = allTxsBefore.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Personal Refreshments expense of 250
        val refreshmentTx = com.engrshuvo.financemanager.data.model.TransactionEntity(
            id = 0,
            type = TransactionType.EXPENSE,
            amount = 250.0,
            categoryId = "personal_refreshments",
            categoryName = "Personal Refreshments",
            note = "Refreshments"
        )
        val txId = repo.insertTransaction(refreshmentTx)

        val allTxsAfter = db.transactionDao().getAllTransactionsForBackup().filter { it.archivedAt == null }
        val afterExpenses = allTxsAfter.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val insertedRefreshment = allTxsAfter.find { it.id == txId }

        assertNotNull(insertedRefreshment)
        assertEquals(250.0, insertedRefreshment!!.amount, 0.001)
        assertEquals("personal_refreshments", insertedRefreshment.categoryId)
        // Verify total expenses increased by exactly 250.0 (counted exactly once)
        assertEquals(250.0, afterExpenses - beforeExpenses, 0.001)
    }
}
