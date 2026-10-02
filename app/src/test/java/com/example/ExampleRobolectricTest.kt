package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CategoryCatalog
import com.example.data.model.TransactionType
import com.example.ui.util.CurrencyUtils
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
}
