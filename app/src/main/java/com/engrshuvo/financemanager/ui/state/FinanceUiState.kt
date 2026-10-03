package com.engrshuvo.financemanager.ui.state

import androidx.compose.runtime.Immutable
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionCategory
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.model.CalendarDayCell
import com.engrshuvo.financemanager.ui.model.DaySummaryStats
import java.util.Calendar

enum class FinanceTab(val label: String) {
    CALENDAR("Calendar"),
    DASHBOARD("Dashboard"),
    LOANS("Loans")
}

enum class TransactionTypeFilter {
    ALL,
    INCOME,
    EXPENSE,
    LOAN
}

enum class DateFilterOption(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    TODAY("Today")
}

enum class LoanTypeFilter {
    ALL,
    LENT,
    BORROWED
}

enum class LoanStatusFilter {
    ACTIVE,
    SETTLED,
    ALL
}

@Immutable
data class CategorySpending(
    val category: TransactionCategory,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

@Immutable
data class CategoryAllocationUiModel(
    val category: TransactionCategory,
    val allocatedAmount: Double,
    val actualSpent: Double,
    val remainingAmount: Double,
    val usagePercentage: Float,
    val isOverBudget: Boolean
)

@Immutable
data class FinanceUiState(
    val activeTab: FinanceTab = FinanceTab.DASHBOARD,

    // Calendar View State
    val displayedMonth: Calendar = Calendar.getInstance(),
    val selectedDateTimestamp: Long = System.currentTimeMillis(),
    val calendarDays: List<CalendarDayCell> = emptyList(),
    val selectedDayTransactions: List<TransactionEntity> = emptyList(),
    val selectedDaySummary: DaySummaryStats? = null,
    val isDayDetailSheetOpen: Boolean = false,

    // Overview / Dashboard State
    val selectedMonthKey: String = "",
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0, // Monthly ordinary income
    val totalExpense: Double = 0.0, // Monthly ordinary expense
    val netOperatingCashChange: Double = 0.0, // totalIncome - totalExpense
    val totalActiveLent: Double = 0.0,
    val totalActiveBorrowed: Double = 0.0,
    
    // Month-Specific Budget Allocations
    val monthAllocations: List<CategoryAllocationUiModel> = emptyList(),
    val totalAllocated: Double = 0.0,
    val unallocatedIncome: Double = 0.0,
    val plannedShortfall: Double = 0.0,
    val isShortfall: Boolean = false,
    val plannedSavingsTotal: Double = 0.0,

    // Daily Spending State
    val todayExpenses: Double = 0.0,
    val dailyBudgetLimit: Double = 500.0,
    val dailyBudgetRemaining: Double = 500.0,
    val dailyBudgetProgress: Float = 0.0f,
    val isDailyOverBudget: Boolean = false,
    val suggestedDailyLimit: Double = 0.0,

    // Legacy / Overall monthly limit fallback
    val monthlyLimit: Double = 30000.0,
    val monthlySpent: Double = 0.0,
    val budgetProgress: Float = 0.0f,
    val budgetRemaining: Double = 30000.0,
    val allTransactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val categorySpendBreakdown: List<CategorySpending> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val selectedCategoryFilterId: String? = null,
    val selectedDateFilter: DateFilterOption = DateFilterOption.ALL_TIME,

    // Loans State
    val allLoans: List<LoanEntity> = emptyList(),
    val filteredLoans: List<LoanEntity> = emptyList(),
    val selectedLoanFilter: LoanTypeFilter = LoanTypeFilter.ALL,
    val selectedLoanStatusFilter: LoanStatusFilter = LoanStatusFilter.ACTIVE,
    val repayingLoan: LoanEntity? = null,
    val isRepayDialogOpen: Boolean = false,

    // Entry & Dialog States
    val isAddTransactionSheetOpen: Boolean = false,
    val editingTransaction: TransactionEntity? = null,
    val defaultEntryType: TransactionType = TransactionType.EXPENSE,
    val defaultLoanType: LoanType = LoanType.LENT,
    val isBudgetLimitDialogOpen: Boolean = false,
    val isBudgetPlanningDialogOpen: Boolean = false,
    val isDailyLimitDialogOpen: Boolean = false,
    val currencySymbol: String = "৳"
)

