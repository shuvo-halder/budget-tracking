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
import org.junit.Assert.assertEquals
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
    fun `bottom navigation tab order has dashboard in exact center and more at rightmost`() {
        val tabs = com.engrshuvo.financemanager.ui.state.FinanceTab.values()
        assertEquals(5, tabs.size)
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.TRANSACTIONS, tabs[0])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.BUDGET, tabs[1])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.DASHBOARD, tabs[2])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.LOANS, tabs[3])
        assertEquals(com.engrshuvo.financemanager.ui.state.FinanceTab.MORE, tabs[4])
    }
}
