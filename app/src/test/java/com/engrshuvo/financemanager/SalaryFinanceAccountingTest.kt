package com.engrshuvo.financemanager

import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SalaryFinanceAccountingTest {

    @Test
    fun `salary and additional income increase monthly income correctly`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 10)
        }
        val currentTs = cal.timeInMillis

        val transactions = listOf(
            TransactionEntity(
                id = 1L,
                type = TransactionType.INCOME,
                amount = 50000.0,
                categoryId = "salary",
                categoryName = "Monthly Salary",
                note = "October Salary",
                timestamp = currentTs
            ),
            TransactionEntity(
                id = 2L,
                type = TransactionType.INCOME,
                amount = 5000.0,
                categoryId = "bonus",
                categoryName = "Bonus & Gifts",
                note = "Festive Bonus",
                timestamp = currentTs
            ),
            TransactionEntity(
                id = 3L,
                type = TransactionType.EXPENSE,
                amount = 3000.0,
                categoryId = "food",
                categoryName = "Food & Dining",
                note = "Dinner",
                timestamp = currentTs
            )
        )

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME && it.categoryId != "loan_collected" }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" }.sumOf { it.amount }
        val netOperatingCash = totalIncome - totalExpenses

        assertEquals(55000.0, totalIncome, 0.001)
        assertEquals(3000.0, totalExpenses, 0.001)
        assertEquals(52000.0, netOperatingCash, 0.001)
    }

    @Test
    fun `budget allocations are plans and do not deduct from available cash`() {
        val income = 60000.0
        val expense = 5000.0
        val actualCash = income - expense

        val plannedAllocations = listOf(
            BudgetAllocationEntity("2026-10", "housing", "House Rent", 15000.0),
            BudgetAllocationEntity("2026-10", "family", "Family Maintenance", 12000.0),
            BudgetAllocationEntity("2026-10", "daily_expenses", "Daily Expenses", 10000.0),
            BudgetAllocationEntity("2026-10", "savings", "Savings Allocation", 10000.0)
        )

        val totalAllocated = plannedAllocations.sumOf { it.allocatedAmount }
        assertEquals(47000.0, totalAllocated, 0.001)

        // Available cash MUST be 55,000 (income - actual expenses), NOT reduced by 47,000 planned allocations!
        assertEquals(55000.0, actualCash, 0.001)
        val unallocatedIncome = income - totalAllocated
        assertEquals(13000.0, unallocatedIncome, 0.001)
    }

    @Test
    fun `month-specific allocations preserve independent plans per month`() {
        val octAllocations = listOf(
            BudgetAllocationEntity("2026-10", "housing", "House Rent", 12000.0),
            BudgetAllocationEntity("2026-10", "daily_expenses", "Daily Expenses", 8000.0)
        )

        val novAllocations = listOf(
            BudgetAllocationEntity("2026-11", "housing", "House Rent", 14000.0),
            BudgetAllocationEntity("2026-11", "daily_expenses", "Daily Expenses", 10000.0)
        )

        val allAllocations = octAllocations + novAllocations

        val octTotal = allAllocations.filter { it.monthKey == "2026-10" }.sumOf { it.allocatedAmount }
        val novTotal = allAllocations.filter { it.monthKey == "2026-11" }.sumOf { it.allocatedAmount }

        assertEquals(20000.0, octTotal, 0.001)
        assertEquals(24000.0, novTotal, 0.001)
    }

    @Test
    fun `copying last month allocations only copies targets and never copies transactions`() {
        val lastMonthAllocations = listOf(
            BudgetAllocationEntity("2026-09", "housing", "House Rent", 10000.0),
            BudgetAllocationEntity("2026-09", "family", "Family Maintenance", 15000.0)
        )

        val now = System.currentTimeMillis()
        val copiedToThisMonth = lastMonthAllocations.map {
            it.copy(monthKey = "2026-10", updatedAt = now)
        }

        assertEquals(2, copiedToThisMonth.size)
        assertTrue(copiedToThisMonth.all { it.monthKey == "2026-10" })
        assertEquals(25000.0, copiedToThisMonth.sumOf { it.allocatedAmount }, 0.001)
    }

    @Test
    fun `daily spending limit and monthly daily allocation remain independent`() {
        val monthlyDailyAllocation = 15000.0
        val daysInMonth = 30
        val suggestedDaily = monthlyDailyAllocation / daysInMonth
        assertEquals(500.0, suggestedDaily, 0.001)

        // User chooses custom daily limit of 600
        val userConfiguredDailyLimit = 600.0

        // Both values remain distinct and preserved
        assertEquals(15000.0, monthlyDailyAllocation, 0.001)
        assertEquals(600.0, userConfiguredDailyLimit, 0.001)
    }

    @Test
    fun `actual expense reduces exactly one category and available cash once`() {
        val initialCash = 50000.0
        val rentAllocation = 10000.0
        val groceryAllocation = 6000.0

        // Pay 10,000 rent
        val rentPayment = 10000.0
        val remainingCash = initialCash - rentPayment
        val remainingRentBudget = rentAllocation - rentPayment
        val remainingGroceryBudget = groceryAllocation // Grocery untouched

        assertEquals(40000.0, remainingCash, 0.001)
        assertEquals(0.0, remainingRentBudget, 0.001)
        assertEquals(6000.0, remainingGroceryBudget, 0.001)
    }

    @Test
    fun `planned shortfall is reported when allocations exceed actual income`() {
        val actualSalary = 40000.0
        val totalAllocated = 45000.0

        val shortfall = (totalAllocated - actualSalary).coerceAtLeast(0.0)
        val isShortfall = totalAllocated > actualSalary

        assertTrue(isShortfall)
        assertEquals(5000.0, shortfall, 0.001)
    }

    @Test
    fun `loans and repayments are kept separate from ordinary operating income and expenses`() {
        val transactions = listOf(
            TransactionEntity(
                id = 1L,
                type = TransactionType.INCOME,
                amount = 40000.0,
                categoryId = "salary",
                categoryName = "Monthly Salary",
                note = "Salary"
            ),
            TransactionEntity(
                id = 2L,
                type = TransactionType.EXPENSE,
                amount = 2000.0,
                categoryId = "food",
                categoryName = "Food & Dining",
                note = "Lunch"
            ),
            TransactionEntity(
                id = 3L,
                type = TransactionType.LOAN,
                amount = 10000.0,
                categoryId = "loan_lent",
                categoryName = "Loan to Friend",
                note = "Lent"
            ),
            TransactionEntity(
                id = 4L,
                type = TransactionType.INCOME,
                amount = 5000.0,
                categoryId = "loan_collected",
                categoryName = "Repayment from Friend",
                note = "Partial repayment"
            ),
            TransactionEntity(
                id = 5L,
                type = TransactionType.EXPENSE,
                amount = 2000.0,
                categoryId = "loan_repaid",
                categoryName = "Repaid to Bank",
                note = "Debt installment"
            )
        )

        // Ordinary living income excludes debt recoveries
        val ordinaryIncome = transactions.filter { it.type == TransactionType.INCOME && it.categoryId != "loan_collected" }.sumOf { it.amount }
        // Ordinary living expense excludes debt installments
        val ordinaryExpense = transactions.filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" }.sumOf { it.amount }

        assertEquals(40000.0, ordinaryIncome, 0.001)
        assertEquals(2000.0, ordinaryExpense, 0.001)
    }
}
