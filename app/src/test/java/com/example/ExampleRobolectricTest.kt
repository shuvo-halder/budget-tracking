package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CategoryCatalog
import com.example.data.model.LoanEntity
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import com.example.ui.util.CurrencyUtils
import com.example.ui.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

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
    fun `loan entity correctly calculates remaining amount`() {
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
}
