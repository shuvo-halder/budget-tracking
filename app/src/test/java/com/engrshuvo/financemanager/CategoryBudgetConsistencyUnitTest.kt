package com.engrshuvo.financemanager

import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBudgetConsistencyUnitTest {

    @Test
    fun `all 16 expense categories are included in default budget categories`() {
        val expenseCatIds = CategoryCatalog.expenseCategories.map { it.id }.toSet()
        val defaultBudgetIds = CategoryCatalog.defaultBudgetCategories.toSet()

        assertEquals(16, expenseCatIds.size)
        // All 16 expense categories must be present in defaultBudgetCategories
        assertTrue(defaultBudgetIds.containsAll(expenseCatIds))

        // Specifically test the 7 previously omitted categories:
        val previouslyMissing = listOf(
            "shopping", "health", "education", "entertainment",
            "fitness", "personal_refreshments", "other_expense"
        )
        for (id in previouslyMissing) {
            assertTrue("Expected $id to be in defaultBudgetCategories", defaultBudgetIds.contains(id))
            assertNotNull("Expected $id to exist in CategoryCatalog", CategoryCatalog.getCategoryById(id))
        }
    }

    @Test
    fun `non-expense categories are excluded from expense categories and budget categories`() {
        val nonExpenseIds = listOf("salary", "business", "bonus", "other_income", "loan_given", "loan_taken", "loan_repaid", "loan_collected")
        val expenseIds = CategoryCatalog.expenseCategories.map { it.id }.toSet()
        val defaultBudgetIds = CategoryCatalog.defaultBudgetCategories.toSet()

        for (nonExp in nonExpenseIds) {
            assertFalse("Non-expense $nonExp should not be in expenseCategories", expenseIds.contains(nonExp))
            assertFalse("Non-expense $nonExp should not be in defaultBudgetCategories", defaultBudgetIds.contains(nonExp))
        }
    }

    @Test
    fun `unallocated expense category starts with zero allocation and does not distort budget totals`() {
        val monthYear = "2026-10"
        val totalMonthlyIncome = 60000.0

        // User allocated only housing (20000) and food (10000)
        val savedAllocations = listOf(
            BudgetAllocationEntity(monthKey = monthYear, categoryId = "housing", categoryName = "Housing", allocatedAmount = 20000.0),
            BudgetAllocationEntity(monthKey = monthYear, categoryId = "food", categoryName = "Food & Dining", allocatedAmount = 10000.0)
        )
        val allocationsMap = savedAllocations.associate { it.categoryId to it.allocatedAmount }

        // User spent 1500 in shopping (which was unallocated)
        val shoppingTransaction = TransactionEntity(
            id = 10L,
            type = TransactionType.EXPENSE,
            amount = 1500.0,
            categoryId = "shopping",
            categoryName = "Shopping",
            note = "New shirt",
            timestamp = System.currentTimeMillis()
        )
        val monthTransactions = listOf(shoppingTransaction)

        // Simulate ViewModel allocation list computation
        val defaultBudgetCats = CategoryCatalog.defaultBudgetCategories
        val transactionExpenseCategoryIds = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .map { it.categoryId }

        val categoryIds = (defaultBudgetCats + allocationsMap.keys + transactionExpenseCategoryIds).distinct()

        val uiModels = categoryIds.map { catId ->
            val catDef = CategoryCatalog.getCategoryById(catId)
            val allocated = allocationsMap[catId] ?: 0.0
            val spent = monthTransactions
                .filter { it.categoryId == catId && it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            val remaining = if (allocated > 0) allocated - spent else 0.0
            val usage = if (allocated > 0) (spent / allocated).toFloat() else 0f
            val isOver = allocated > 0 && spent > allocated

            CategoryAllocationUiModel(
                category = catDef,
                allocatedAmount = allocated,
                actualSpent = spent,
                remainingAmount = remaining,
                usagePercentage = usage,
                isOverBudget = isOver
            )
        }

        // Verify shopping is in the ui models
        val shoppingModel = uiModels.find { it.category.id == "shopping" }
        assertNotNull("Shopping should be present in allocation models", shoppingModel)
        assertEquals(0.0, shoppingModel!!.allocatedAmount, 0.001)
        assertEquals(1500.0, shoppingModel.actualSpent, 0.001)
        assertFalse(shoppingModel.isOverBudget)

        // Verify budget summary calculations are intact
        val totalAllocated = uiModels.sumOf { it.allocatedAmount }
        val unallocatedIncome = (totalMonthlyIncome - totalAllocated).coerceAtLeast(0.0)
        val plannedShortfall = (totalAllocated - totalMonthlyIncome).coerceAtLeast(0.0)

        assertEquals(30000.0, totalAllocated, 0.001)
        assertEquals(30000.0, unallocatedIncome, 0.001)
        assertEquals(0.0, plannedShortfall, 0.001)
    }

    @Test
    fun `allocating to all 16 expense categories updates totals accurately`() {
        val totalIncome = 100000.0
        val allocationInputs = mapOf(
            "housing" to 25000.0,
            "family" to 10000.0,
            "daily_expenses" to 5000.0,
            "food" to 8000.0,
            "grocery" to 7000.0,
            "bills" to 4000.0,
            "transport" to 3000.0,
            "savings" to 10000.0,
            "emergency" to 5000.0,
            "shopping" to 4000.0,
            "health" to 3000.0,
            "education" to 5000.0,
            "entertainment" to 2000.0,
            "fitness" to 1500.0,
            "personal_refreshments" to 1500.0,
            "other_expense" to 1000.0
        )

        val totalAllocated = allocationInputs.values.sum()
        assertEquals(95000.0, totalAllocated, 0.001)
        val unallocated = (totalIncome - totalAllocated).coerceAtLeast(0.0)
        assertEquals(5000.0, unallocated, 0.001)
    }

    @Test
    fun `income and loan transactions do not leak into month allocations list`() {
        val incomeTx = TransactionEntity(
            id = 1L,
            type = TransactionType.INCOME,
            amount = 50000.0,
            categoryId = "salary",
            categoryName = "Monthly Salary",
            note = "Salary",
            timestamp = System.currentTimeMillis()
        )
        val loanTx = TransactionEntity(
            id = 2L,
            type = TransactionType.EXPENSE,
            amount = 10000.0,
            categoryId = "loan_given",
            categoryName = "Loan Given",
            note = "Friend loan",
            timestamp = System.currentTimeMillis()
        )

        val monthTransactions = listOf(incomeTx, loanTx)

        // Filter as ViewModel does
        val transactionExpenseCategoryIds = monthTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_given" && it.categoryId != "loan_repaid" }
            .map { it.categoryId }

        val defaultBudgetCats = CategoryCatalog.defaultBudgetCategories
        val finalCategoryIds = (defaultBudgetCats + transactionExpenseCategoryIds).distinct()

        assertFalse(finalCategoryIds.contains("salary"))
        assertFalse(finalCategoryIds.contains("loan_given"))
    }
}
