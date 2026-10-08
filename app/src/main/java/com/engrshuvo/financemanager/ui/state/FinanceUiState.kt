package com.engrshuvo.financemanager.ui.state

import androidx.compose.runtime.Immutable
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.FinancialGoalUiModel
import com.engrshuvo.financemanager.data.model.GoalStatus
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
    TRANSACTIONS("Transactions"),
    BUDGET("Budget"),
    DASHBOARD("Dashboard"),
    CALENDAR("Calendar"),
    LOANS("Loans")
}

enum class MoreSubDestination {
    NONE,
    CALENDAR,
    ARCHIVE,
    REPORTS,
    NOTIFICATION_SETTINGS,
    BACKUP_RESTORE,
    GOALS
}

enum class ArchiveFilterType(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense"),
    LOANS("Loans"),
    GOALS("Goals")
}

@Immutable
sealed interface ArchiveItemWrapper {
    val id: Long
    val title: String
    val amount: Double
    val originalDate: Long
    val archivedAt: Long
    val daysRemaining: Int
    val formattedRetentionRemaining: String

    data class Transaction(
        val entity: TransactionEntity,
        override val id: Long = entity.id,
        override val title: String = entity.categoryName,
        override val amount: Double = entity.amount,
        override val originalDate: Long = entity.timestamp,
        override val archivedAt: Long = entity.archivedAt ?: System.currentTimeMillis(),
        override val daysRemaining: Int,
        override val formattedRetentionRemaining: String
    ) : ArchiveItemWrapper

    data class Loan(
        val entity: LoanEntity,
        override val id: Long = entity.id,
        override val title: String = "${if (entity.type == LoanType.LENT) "Loan to" else "Loan from"} ${entity.personName}",
        override val amount: Double = entity.initialAmount,
        override val originalDate: Long = entity.startDate,
        override val archivedAt: Long = entity.archivedAt ?: System.currentTimeMillis(),
        override val daysRemaining: Int,
        override val formattedRetentionRemaining: String
    ) : ArchiveItemWrapper

    data class Goal(
        val entity: FinancialGoalEntity,
        override val id: Long = entity.id,
        override val title: String = "Goal: ${entity.name}",
        override val amount: Double = entity.targetAmount,
        override val originalDate: Long = entity.createdAt,
        override val archivedAt: Long = entity.archivedAt ?: System.currentTimeMillis(),
        override val daysRemaining: Int,
        override val formattedRetentionRemaining: String
    ) : ArchiveItemWrapper
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
    val moreSubDestination: MoreSubDestination = MoreSubDestination.NONE,

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
    val isBudgetAllocationDraft: Boolean = false,
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

    // Financial Goals State
    val allGoals: List<FinancialGoalUiModel> = emptyList(),
    val activeGoals: List<FinancialGoalUiModel> = emptyList(),
    val totalGoalTarget: Double = 0.0,
    val totalGoalSaved: Double = 0.0,
    val overallGoalProgressPercent: Double = 0.0,
    val thisMonthGoalSaved: Double = 0.0,
    val totalMonthlyGoalRequired: Double = 0.0,
    val monthlyGoalSavingCapacity: Double = 0.0,
    val isGoalCapacityDeficit: Boolean = false,
    val goalCapacityDifference: Double = 0.0,
    val selectedGoalFilterStatus: GoalStatus? = null,
    val selectedGoalDetailsId: Long? = null,
    val isCreateGoalDialogOpen: Boolean = false,
    val editingGoal: FinancialGoalEntity? = null,
    val contributingGoal: FinancialGoalEntity? = null,
    val isAddContributionDialogOpen: Boolean = false,

    // Archive & Recovery State
    val archivedItems: List<ArchiveItemWrapper> = emptyList(),
    val filteredArchivedItems: List<ArchiveItemWrapper> = emptyList(),
    val archiveFilterType: ArchiveFilterType = ArchiveFilterType.ALL,
    val archiveSearchQuery: String = "",
    val itemToPermanentlyDelete: ArchiveItemWrapper? = null,
    val recentlyArchivedNote: String? = null,

    // Entry & Dialog States
    val isAddTransactionSheetOpen: Boolean = false,
    val editingTransaction: TransactionEntity? = null,
    val defaultEntryType: TransactionType = TransactionType.EXPENSE,
    val defaultLoanType: LoanType = LoanType.LENT,
    val isBudgetLimitDialogOpen: Boolean = false,
    val isBudgetPlanningDialogOpen: Boolean = false,
    val isDailyLimitDialogOpen: Boolean = false,
    val currencySymbol: String = "৳",

    // Notification & Reminders State
    val notificationPreferences: com.engrshuvo.financemanager.notification.NotificationPreferences = com.engrshuvo.financemanager.notification.NotificationPreferences(),
    val notificationPermissionGranted: Boolean = true,

    // Backup & Restore State
    val isExportingBackup: Boolean = false,
    val isRestoringBackup: Boolean = false,
    val backupPreview: com.engrshuvo.financemanager.data.model.BackupSummaryPreview? = null,
    val pendingRestoreData: com.engrshuvo.financemanager.data.model.FinanceBackupData? = null,
    val backupOperationMessage: String? = null,
    val backupOperationError: String? = null
)
