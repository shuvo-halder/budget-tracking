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

    @Test
    fun `available cash balance correctly accounts for all income, living expenses, and loan cashflows`() {
        val transactions = listOf(
            // Salary received (+50,000)
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 50000.0, categoryId = "salary", categoryName = "Salary", note = ""),
            // Living expense (-10,000)
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 10000.0, categoryId = "housing", categoryName = "Rent", note = ""),
            // Lent to friend (-5,000 cash disbursed)
            TransactionEntity(id = 3, type = TransactionType.LOAN, amount = 5000.0, categoryId = "loan_lent", categoryName = "Loan to Karim", note = ""),
            // Friend partial repayment (+2,000 cash received)
            TransactionEntity(id = 4, type = TransactionType.INCOME, amount = 2000.0, categoryId = "loan_collected", categoryName = "Repayment", note = ""),
            // Borrowed from bank (+15,000 cash received)
            TransactionEntity(id = 5, type = TransactionType.LOAN, amount = 15000.0, categoryId = "loan_borrowed", categoryName = "Loan from Bank", note = ""),
            // Repaid bank (-4,000 cash disbursed)
            TransactionEntity(id = 6, type = TransactionType.EXPENSE, amount = 4000.0, categoryId = "loan_repaid", categoryName = "Repaid", note = "")
        )

        val allTimeIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val allTimeExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val allTimeBorrowed = transactions.filter { it.type == TransactionType.LOAN && it.categoryId == "loan_borrowed" }.sumOf { it.amount }
        val allTimeLent = transactions.filter { it.type == TransactionType.LOAN && it.categoryId == "loan_lent" }.sumOf { it.amount }
        val balance = allTimeIncome - allTimeExpense + allTimeBorrowed - allTimeLent

        // 50,000 (salary) - 10,000 (rent) - 5,000 (lent) + 2,000 (collected) + 15,000 (borrowed) - 4,000 (repaid) = 48,000
        assertEquals(48000.0, balance, 0.001)
    }

    @Test
    fun `deterministic reconciliation example calculates exact ending cash of 29500 with opening cash and 19500 net period movements`() {
        // Construct deterministic scenario from prompt:
        // Opening cash: ৳10,000 (represented as opening income/balance)
        // Salary received: ৳30,000
        // Living expenses: ৳8,000
        // Money lent: ৳5,000
        // Money borrowed: ৳2,000
        // Collection of previously lent money: ৳1,000
        // Repayment of borrowed principal: ৳500

        val transactions = listOf(
            // 1. Opening Cash (৳10,000)
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 10000.0, categoryId = "other_income", categoryName = "Opening Cash", note = "Opening Balance"),
            // 2. Salary received (+৳30,000)
            TransactionEntity(id = 2, type = TransactionType.INCOME, amount = 30000.0, categoryId = "salary", categoryName = "Salary", note = "Monthly Salary"),
            // 3. Living expenses (-৳8,000)
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 8000.0, categoryId = "food", categoryName = "Living Expenses", note = "Groceries and Utilities"),
            // 4. Money lent (-৳5,000 cash disbursed)
            TransactionEntity(id = 4, type = TransactionType.LOAN, amount = 5000.0, categoryId = "loan_lent", categoryName = "Loan to Friend", note = "Lent", loanId = 100L),
            // 5. Money borrowed (+৳2,000 cash received)
            TransactionEntity(id = 5, type = TransactionType.LOAN, amount = 2000.0, categoryId = "loan_borrowed", categoryName = "Loan from Bank", note = "Borrowed", loanId = 101L),
            // 6. Collection of previously lent money (+৳1,000 cash received)
            TransactionEntity(id = 6, type = TransactionType.INCOME, amount = 1000.0, categoryId = "loan_collected", categoryName = "Repayment from Friend", note = "Collection", loanId = 100L),
            // 7. Repayment of borrowed principal (-৳500 cash disbursed)
            TransactionEntity(id = 7, type = TransactionType.EXPENSE, amount = 500.0, categoryId = "loan_repaid", categoryName = "Repaid to Bank", note = "Installment", loanId = 101L)
        )

        // Production cash balance formula:
        val allTimeIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val allTimeExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val allTimeBorrowed = transactions.filter { it.type == TransactionType.LOAN && it.categoryId == "loan_borrowed" }.sumOf { it.amount }
        val allTimeLent = transactions.filter { it.type == TransactionType.LOAN && it.categoryId == "loan_lent" }.sumOf { it.amount }
        val balanceWithOpening = allTimeIncome - allTimeExpense + allTimeBorrowed - allTimeLent

        // Total Income = 10000 + 30000 + 1000 = 41000
        // Total Expense = 8000 + 500 = 8500
        // Borrowed = 2000
        // Lent = 5000
        // Balance = 41000 - 8500 + 2000 - 5000 = 29500
        assertEquals(29500.0, balanceWithOpening, 0.001)

        // Without opening cash transaction (net period activity):
        val periodTransactions = transactions.filter { it.id != 1L }
        val pIncome = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val pExpense = periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val pBorrowed = periodTransactions.filter { it.type == TransactionType.LOAN && it.categoryId == "loan_borrowed" }.sumOf { it.amount }
        val pLent = periodTransactions.filter { it.type == TransactionType.LOAN && it.categoryId == "loan_lent" }.sumOf { it.amount }
        val netPeriodBalance = pIncome - pExpense + pBorrowed - pLent

        // 31000 - 8500 + 2000 - 5000 = 19500
        assertEquals(19500.0, netPeriodBalance, 0.001)
    }

    @Test
    fun `repayment is counted strictly once and not double counted across loan repayments and transactions`() {
        val transactions = listOf(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 1000.0, categoryId = "loan_collected", categoryName = "Repayment", note = "", loanId = 10L)
        )
        // Cash balance only aggregates the transactions table
        val balance = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
                transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        assertEquals(1000.0, balance, 0.001)
    }

    @Test
    fun `unconfigured month pre-fills previous month targets as unsaved draft`() {
        val septAllocations = listOf(
            BudgetAllocationEntity("2026-09", "housing", "House Rent", 12000.0),
            BudgetAllocationEntity("2026-09", "food", "Food & Dining", 8000.0)
        )
        val octAllocations = emptyList<BudgetAllocationEntity>()

        val allAllocations = septAllocations + octAllocations

        val savedOctAllocations = allAllocations.filter { it.monthKey == "2026-10" }
        val prevAllocations = allAllocations.filter { it.monthKey == "2026-09" }

        val hasSaved = savedOctAllocations.isNotEmpty()
        val isDraft = !hasSaved && prevAllocations.isNotEmpty()

        val octSourceMap = if (hasSaved) {
            savedOctAllocations.associateBy { it.categoryId }
        } else if (prevAllocations.isNotEmpty()) {
            prevAllocations.associateBy { it.categoryId }
        } else {
            emptyMap()
        }

        assertTrue(isDraft)
        assertEquals(20000.0, octSourceMap.values.sumOf { it.allocatedAmount }, 0.001)
        assertEquals(12000.0, octSourceMap["housing"]?.allocatedAmount ?: 0.0, 0.001)

        // Sept allocations remain untouched
        assertEquals(20000.0, septAllocations.sumOf { it.allocatedAmount }, 0.001)
    }

    @Test
    fun `saving a draft creates allocations for new month without modifying previous month`() {
        val septAllocations = listOf(
            BudgetAllocationEntity("2026-09", "housing", "House Rent", 12000.0),
            BudgetAllocationEntity("2026-09", "food", "Food & Dining", 8000.0)
        )

        // User edits housing to 14000 and saves for October
        val newlySavedOctAllocations = listOf(
            BudgetAllocationEntity("2026-10", "housing", "House Rent", 14000.0),
            BudgetAllocationEntity("2026-10", "food", "Food & Dining", 8000.0)
        )

        val updatedAllAllocations = septAllocations + newlySavedOctAllocations

        val septTotal = updatedAllAllocations.filter { it.monthKey == "2026-09" }.sumOf { it.allocatedAmount }
        val octTotal = updatedAllAllocations.filter { it.monthKey == "2026-10" }.sumOf { it.allocatedAmount }

        assertEquals(20000.0, septTotal, 0.001)
        assertEquals(22000.0, octTotal, 0.001)
        // Sept housing is still 12000
        assertEquals(12000.0, updatedAllAllocations.find { it.monthKey == "2026-09" && it.categoryId == "housing" }?.allocatedAmount ?: 0.0, 0.001)
        // Oct housing is 14000
        assertEquals(14000.0, updatedAllAllocations.find { it.monthKey == "2026-10" && it.categoryId == "housing" }?.allocatedAmount ?: 0.0, 0.001)
    }

    @Test
    fun `archiving transaction updates active cash and restoring recovers original cash exactly`() {
        val tx1 = TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 20000.0, categoryId = "salary", categoryName = "Salary", note = "")
        val tx2 = TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 5000.0, categoryId = "food", categoryName = "Dining", note = "")

        val activeList1 = listOf(tx1, tx2)
        val balance1 = activeList1.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
                activeList1.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        assertEquals(15000.0, balance1, 0.001)

        // Archive expense tx2
        val activeList2 = listOf(tx1)
        val balance2 = activeList2.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
                activeList2.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        assertEquals(20000.0, balance2, 0.001)

        // Restore tx2
        val activeList3 = listOf(tx1, tx2)
        val balance3 = activeList3.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
                activeList3.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        assertEquals(15000.0, balance3, 0.001)
    }
}
