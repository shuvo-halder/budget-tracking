package com.engrshuvo.financemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.FinancialGoalUiModel
import com.engrshuvo.financemanager.data.model.GoalCategory
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import com.engrshuvo.financemanager.data.model.GoalPriority
import com.engrshuvo.financemanager.data.model.GoalStatus
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.PurchaseItemEntity
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.ui.model.CalendarDayCell
import com.engrshuvo.financemanager.ui.model.DaySummaryStats
import com.engrshuvo.financemanager.ui.state.ArchiveFilterType
import com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import com.engrshuvo.financemanager.ui.state.CategorySpending
import com.engrshuvo.financemanager.ui.state.DateFilterOption
import com.engrshuvo.financemanager.ui.state.FinanceTab
import com.engrshuvo.financemanager.ui.state.FinanceUiState
import com.engrshuvo.financemanager.ui.state.LoanStatusFilter
import com.engrshuvo.financemanager.ui.state.LoanTypeFilter
import com.engrshuvo.financemanager.ui.state.MoreSubDestination
import com.engrshuvo.financemanager.ui.state.PurchaseItemFilter
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.util.DateUtils
import android.content.Context
import com.engrshuvo.financemanager.notification.NotificationPreferences
import com.engrshuvo.financemanager.notification.NotificationPreferencesRepository
import com.engrshuvo.financemanager.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val notificationPreferencesRepo: NotificationPreferencesRepository? = null,
    private val paymentSmsPreferencesRepo: com.engrshuvo.financemanager.sms.PaymentSmsPreferencesRepository? = null
) : ViewModel() {

    private val _activeTab = MutableStateFlow(FinanceTab.DASHBOARD)
    private val _moreSubDestination = MutableStateFlow(MoreSubDestination.NONE)

    private val _displayedMonth = MutableStateFlow(Calendar.getInstance())
    private val _selectedDateTimestamp = MutableStateFlow(System.currentTimeMillis())
    private val _isDayDetailSheetOpen = MutableStateFlow(false)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    private val _selectedCategoryFilterId = MutableStateFlow<String?>(null)
    private val _selectedDateFilter = MutableStateFlow(DateFilterOption.ALL_TIME)

    private val _selectedLoanFilter = MutableStateFlow(LoanTypeFilter.ALL)
    private val _selectedLoanStatusFilter = MutableStateFlow(LoanStatusFilter.ACTIVE)
    private val _repayingLoan = MutableStateFlow<LoanEntity?>(null)
    private val _isRepayDialogOpen = MutableStateFlow(false)
    private val _isRepaymentSavingState = MutableStateFlow(false)
    private val _loanOperationError = MutableStateFlow<String?>(null)

    private data class TransactionSheetState(
        val isOpen: Boolean = false,
        val editingTransaction: TransactionEntity? = null,
        val defaultEntryType: TransactionType = TransactionType.EXPENSE,
        val defaultLoanType: LoanType = LoanType.LENT
    )

    private val _transactionSheetState = MutableStateFlow(TransactionSheetState())
    private val _isBudgetLimitDialogOpen = MutableStateFlow(false)
    private val _isBudgetPlanningDialogOpen = MutableStateFlow(false)
    private val _isDailyLimitDialogOpen = MutableStateFlow(false)

    // Financial Goals State
    private val _selectedGoalFilterStatus = MutableStateFlow<GoalStatus?>(null)
    private val _selectedGoalDetailsId = MutableStateFlow<Long?>(null)
    private val _isCreateGoalDialogOpen = MutableStateFlow(false)
    private val _editingGoal = MutableStateFlow<FinancialGoalEntity?>(null)
    private val _contributingGoal = MutableStateFlow<FinancialGoalEntity?>(null)
    private val _isAddContributionDialogOpen = MutableStateFlow(false)

    private val isGoalSaving = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isContributionSaving = java.util.concurrent.atomic.AtomicBoolean(false)

    // Archive & Recovery state flows
    private val _archiveFilterType = MutableStateFlow(ArchiveFilterType.ALL)
    private val _archiveSearchQuery = MutableStateFlow("")
    private val _itemToPermanentlyDelete = MutableStateFlow<ArchiveItemWrapper?>(null)
    private val _recentlyArchivedNote = MutableStateFlow<String?>(null)

    // Notification State
    private val _notificationPrefs = MutableStateFlow(NotificationPreferences())
    private val _notificationPermissionGranted = MutableStateFlow(true)

    // Backup & Restore State
    private val _isExportingBackup = MutableStateFlow(false)
    private val _isRestoringBackup = MutableStateFlow(false)
    private val _backupPreview = MutableStateFlow<com.engrshuvo.financemanager.data.model.BackupSummaryPreview?>(null)
    private val _pendingRestoreData = MutableStateFlow<com.engrshuvo.financemanager.data.model.FinanceBackupData?>(null)
    private val _backupOperationMessage = MutableStateFlow<String?>(null)
    private val _backupOperationError = MutableStateFlow<String?>(null)

    // PDF Export Center State
    private val _pdfFilterState = MutableStateFlow(com.engrshuvo.financemanager.data.model.PdfExportFilterState())
    private val _isGeneratingPdf = MutableStateFlow(false)
    private val _pdfExportMessage = MutableStateFlow<String?>(null)
    private val _pdfExportError = MutableStateFlow<String?>(null)
    private val _pdfExportFileUri = MutableStateFlow<android.net.Uri?>(null)

    // Purchase Planner (Shopping Notes) State
    private val _purchaseItemFilter = MutableStateFlow(PurchaseItemFilter.PENDING)
    private val _purchaseSearchQuery = MutableStateFlow("")
    private val _isAddPurchaseDialogOpen = MutableStateFlow(false)
    private val _editingPurchaseItem = MutableStateFlow<PurchaseItemEntity?>(null)
    private val _itemToDeletePurchase = MutableStateFlow<PurchaseItemEntity?>(null)
    private val isPurchaseItemSaving = java.util.concurrent.atomic.AtomicBoolean(false)

    // In-flight operation guards to prevent duplicate concurrent submissions
    private val isTransactionSaving = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isRepaymentSaving = java.util.concurrent.atomic.AtomicBoolean(false)

    // Payment SMS Import State
    private val _smsRecordToReview = MutableStateFlow<com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity?>(null)
    private val _smsPasteInput = MutableStateFlow("")
    private val _smsPasteTestResult = MutableStateFlow<String?>(null)
    private val _smsOperationMessage = MutableStateFlow<String?>(null)

    init {
        // Automatic cleanup on app startup
        viewModelScope.launch {
            repository.purgeExpiredArchivedRecords()
        }
        if (notificationPreferencesRepo != null) {
            viewModelScope.launch {
                notificationPreferencesRepo.preferencesFlow.collect {
                    _notificationPrefs.value = it
                }
            }
        }
    }

    private data class CalendarParams(
        val month: Calendar,
        val selectedDate: Long,
        val isSheetOpen: Boolean
    )

    private data class FilterParams(
        val search: String,
        val type: TransactionTypeFilter,
        val categoryId: String?,
        val dateOption: DateFilterOption,
        val loanType: LoanTypeFilter,
        val loanStatus: LoanStatusFilter
    )

    private data class CoreData(
        val activeTab: FinanceTab,
        val allTransactions: List<TransactionEntity>,
        val allLoans: List<LoanEntity>,
        val monthlyLimit: Double,
        val dailyLimit: Double,
        val allAllocations: List<BudgetAllocationEntity>
    )

    private data class ArchiveParams(
        val filterType: ArchiveFilterType,
        val searchQuery: String,
        val itemToDelete: ArchiveItemWrapper?,
        val moreDest: MoreSubDestination,
        val recentlyArchivedNote: String?
    )

    private data class ArchiveData(
        val archivedTransactions: List<TransactionEntity>,
        val archivedLoans: List<LoanEntity>
    )

    private val budgetDataFlow = combine(
        repository.monthlyBudgetLimit,
        repository.dailyBudgetLimit,
        repository.allBudgetAllocations
    ) { monthlyLimit, dailyLimit, allocations ->
        Triple(monthlyLimit, dailyLimit, allocations)
    }

    private val coreDataFlow: Flow<CoreData> = combine(
        _activeTab,
        repository.allTransactions,
        repository.allLoans,
        budgetDataFlow
    ) { activeTab, transactions, loans, budgetData ->
        CoreData(
            activeTab = activeTab,
            allTransactions = transactions,
            allLoans = loans,
            monthlyLimit = budgetData.first,
            dailyLimit = budgetData.second,
            allAllocations = budgetData.third
        )
    }.flowOn(Dispatchers.Default)

    private val calendarParamsFlow: Flow<CalendarParams> = combine(
        _displayedMonth,
        _selectedDateTimestamp,
        _isDayDetailSheetOpen
    ) { month, date, isSheetOpen ->
        CalendarParams(month, date, isSheetOpen)
    }.flowOn(Dispatchers.Default)

    private val filterParamsFlow: Flow<FilterParams> = combine(
        _searchQuery,
        _selectedTypeFilter,
        _selectedCategoryFilterId,
        _selectedDateFilter,
        _selectedLoanFilter
    ) { search, type, catId, dateOpt, loanType ->
        FilterParams(
            search = search,
            type = type,
            categoryId = catId,
            dateOption = dateOpt,
            loanType = loanType,
            loanStatus = _selectedLoanStatusFilter.value
        )
    }.combine(_selectedLoanStatusFilter) { params, loanStatus ->
        params.copy(loanStatus = loanStatus)
    }.flowOn(Dispatchers.Default)

    private val archiveParamsFlow: Flow<ArchiveParams> = combine(
        _archiveFilterType,
        _archiveSearchQuery,
        _itemToPermanentlyDelete,
        _moreSubDestination,
        _recentlyArchivedNote
    ) { filterType, searchQuery, itemToDelete, moreDest, recentlyArchivedNote ->
        ArchiveParams(
            filterType = filterType,
            searchQuery = searchQuery,
            itemToDelete = itemToDelete,
            moreDest = moreDest,
            recentlyArchivedNote = recentlyArchivedNote
        )
    }.flowOn(Dispatchers.Default)

    private data class DialogParams(
        val isAddTransactionSheetOpen: Boolean,
        val editingTransaction: TransactionEntity?,
        val defaultEntryType: TransactionType,
        val defaultLoanType: LoanType,
        val isRepayDialogOpen: Boolean,
        val repayingLoan: LoanEntity?,
        val isBudgetLimitDialogOpen: Boolean,
        val isBudgetPlanningDialogOpen: Boolean,
        val isDailyLimitDialogOpen: Boolean,
        val isRepaymentSaving: Boolean,
        val loanOperationError: String?
    )

    private val dialogParamsFlow: Flow<DialogParams> = combine(
        listOf(
            _transactionSheetState,
            _isRepayDialogOpen,
            _repayingLoan,
            _isBudgetLimitDialogOpen,
            _isBudgetPlanningDialogOpen,
            _isDailyLimitDialogOpen,
            _isRepaymentSavingState,
            _loanOperationError
        )
    ) { array ->
        val sheet = array[0] as TransactionSheetState
        DialogParams(
            isAddTransactionSheetOpen = sheet.isOpen,
            editingTransaction = sheet.editingTransaction,
            defaultEntryType = sheet.defaultEntryType,
            defaultLoanType = sheet.defaultLoanType,
            isRepayDialogOpen = array[1] as Boolean,
            repayingLoan = array[2] as? LoanEntity,
            isBudgetLimitDialogOpen = array[3] as Boolean,
            isBudgetPlanningDialogOpen = array[4] as Boolean,
            isDailyLimitDialogOpen = array[5] as Boolean,
            isRepaymentSaving = array[6] as Boolean,
            loanOperationError = array[7] as? String
        )
    }.flowOn(Dispatchers.Default)

    private data class BackupNotificationParams(
        val isExportingBackup: Boolean,
        val isRestoringBackup: Boolean,
        val backupPreview: com.engrshuvo.financemanager.data.model.BackupSummaryPreview?,
        val pendingRestoreData: com.engrshuvo.financemanager.data.model.FinanceBackupData?,
        val backupOperationMessage: String?,
        val backupOperationError: String?,
        val notificationPreferences: NotificationPreferences,
        val notificationPermissionGranted: Boolean
    )

    private val backupNotificationParamsFlow: Flow<BackupNotificationParams> = combine(
        listOf(
            _isExportingBackup,
            _isRestoringBackup,
            _backupPreview,
            _pendingRestoreData,
            _backupOperationMessage,
            _backupOperationError,
            _notificationPrefs,
            _notificationPermissionGranted
        )
    ) { array ->
        BackupNotificationParams(
            isExportingBackup = array[0] as Boolean,
            isRestoringBackup = array[1] as Boolean,
            backupPreview = array[2] as? com.engrshuvo.financemanager.data.model.BackupSummaryPreview,
            pendingRestoreData = array[3] as? com.engrshuvo.financemanager.data.model.FinanceBackupData,
            backupOperationMessage = array[4] as? String,
            backupOperationError = array[5] as? String,
            notificationPreferences = array[6] as NotificationPreferences,
            notificationPermissionGranted = array[7] as Boolean
        )
    }.flowOn(Dispatchers.Default)

    private data class PdfParams(
        val filterState: com.engrshuvo.financemanager.data.model.PdfExportFilterState,
        val isGenerating: Boolean,
        val message: String?,
        val error: String?,
        val fileUri: android.net.Uri?
    )

    private val pdfParamsFlow: Flow<PdfParams> = combine(
        listOf(
            _pdfFilterState,
            _isGeneratingPdf,
            _pdfExportMessage,
            _pdfExportError,
            _pdfExportFileUri
        )
    ) { array ->
        PdfParams(
            filterState = array[0] as com.engrshuvo.financemanager.data.model.PdfExportFilterState,
            isGenerating = array[1] as Boolean,
            message = array[2] as? String,
            error = array[3] as? String,
            fileUri = array[4] as? android.net.Uri
        )
    }.flowOn(Dispatchers.Default)

    private data class PurchasePlannerParams(
        val items: List<PurchaseItemEntity>,
        val filter: PurchaseItemFilter,
        val searchQuery: String,
        val isAddDialogOpen: Boolean,
        val editingItem: PurchaseItemEntity?,
        val itemToDelete: PurchaseItemEntity?
    )

    private val purchasePlannerParamsFlow: Flow<PurchasePlannerParams> = combine(
        repository.allPurchaseItems,
        _purchaseItemFilter,
        _purchaseSearchQuery,
        _isAddPurchaseDialogOpen,
        _editingPurchaseItem
    ) { items, filter, query, isAddOpen, editingItem ->
        PurchasePlannerParams(
            items = items,
            filter = filter,
            searchQuery = query,
            isAddDialogOpen = isAddOpen,
            editingItem = editingItem,
            itemToDelete = _itemToDeletePurchase.value
        )
    }.combine(_itemToDeletePurchase) { params, toDelete ->
        params.copy(itemToDelete = toDelete)
    }.flowOn(Dispatchers.Default)

    private data class SmsImportParams(
        val prefs: com.engrshuvo.financemanager.sms.PaymentSmsPreferences,
        val records: List<com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity>,
        val recordToReview: com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity?,
        val pasteInput: String,
        val pasteTestResult: String?,
        val operationMessage: String?
    )

    private val smsImportParamsFlow: Flow<SmsImportParams> = combine(
        paymentSmsPreferencesRepo?.preferencesFlow ?: kotlinx.coroutines.flow.flowOf(com.engrshuvo.financemanager.sms.PaymentSmsPreferences()),
        repository.allSmsImportedRecords,
        _smsRecordToReview,
        _smsPasteInput,
        _smsPasteTestResult
    ) { prefs, records, toReview, pasteInput, testResult ->
        SmsImportParams(
            prefs = prefs,
            records = records,
            recordToReview = toReview,
            pasteInput = pasteInput,
            pasteTestResult = testResult,
            operationMessage = _smsOperationMessage.value
        )
    }.combine(_smsOperationMessage) { params, msg ->
        params.copy(operationMessage = msg)
    }.flowOn(Dispatchers.Default)

    private data class MonthlyFinancialSummary(
        val selectedMonthKey: String,
        val balance: Double,
        val totalIncome: Double,
        val totalExpense: Double,
        val netOperatingCashChange: Double,
        val totalActiveLent: Double,
        val totalActiveBorrowed: Double,
        val monthAllocations: List<CategoryAllocationUiModel>,
        val isBudgetAllocationDraft: Boolean,
        val totalAllocated: Double,
        val unallocatedIncome: Double,
        val plannedShortfall: Double,
        val isShortfall: Boolean,
        val plannedSavingsTotal: Double,
        val todayExpenses: Double,
        val dailyBudgetRemaining: Double,
        val dailyBudgetProgress: Float,
        val isDailyOverBudget: Boolean,
        val suggestedDailyLimit: Double,
        val monthlySpent: Double,
        val budgetProgress: Float,
        val budgetRemaining: Double,
        val categoryBreakdown: List<CategorySpending>,
        val startOfMonth: Long,
        val endOfMonth: Long
    )

    private val monthlySummaryFlow: Flow<MonthlyFinancialSummary> = combine(
        _displayedMonth,
        coreDataFlow
    ) { displayedMonth, coreData ->
        val allTransactions = coreData.allTransactions
        val allLoans = coreData.allLoans
        val monthlyLimit = coreData.monthlyLimit
        val dailyLimit = coreData.dailyLimit
        val allAllocations = coreData.allAllocations

        val cal = displayedMonth.clone() as Calendar
        val year = cal.get(Calendar.YEAR)
        val monthZeroBased = cal.get(Calendar.MONTH)
        val selectedMonthKey = String.format(Locale.US, "%04d-%02d", year, monthZeroBased + 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val startOfMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val monthTransactions = allTransactions.filter { it.timestamp in startOfMonth..endOfMonth }

        val totalIncome = monthTransactions
            .filter { it.type == TransactionType.INCOME && it.categoryId != "loan_collected" && it.categoryId != "loan_borrowed" }
            .sumOf { it.amount }

        val totalExpense = monthTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" && it.categoryId != "loan_lent" }
            .sumOf { it.amount }

        val netOperatingCashChange = totalIncome - totalExpense

        val allTimeIncome = allTransactions.filter { it.type == TransactionType.INCOME && it.categoryId != "loan_collected" && it.categoryId != "loan_borrowed" }.sumOf { it.amount }
        val allTimeExpense = allTransactions.filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" && it.categoryId != "loan_lent" }.sumOf { it.amount }
        val allTimeBorrowed = allTransactions.filter { it.categoryId == "loan_borrowed" }.sumOf { it.amount }
        val allTimeLent = allTransactions.filter { it.categoryId == "loan_lent" }.sumOf { it.amount }
        val allTimeRepaid = allTransactions.filter { it.categoryId == "loan_repaid" }.sumOf { it.amount }
        val allTimeCollected = allTransactions.filter { it.categoryId == "loan_collected" }.sumOf { it.amount }
        val balance = allTimeIncome - allTimeExpense + allTimeBorrowed - allTimeLent - allTimeRepaid + allTimeCollected

        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()
        val todayExpenses = allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" && it.categoryId != "loan_lent" && it.timestamp in startOfToday..endOfToday }
            .sumOf { it.amount }

        val dailyBudgetRemaining = dailyLimit - todayExpenses
        val dailyBudgetProgress = if (dailyLimit > 0) (todayExpenses / dailyLimit).toFloat().coerceIn(0f, 1f) else 0f
        val isDailyOverBudget = todayExpenses > dailyLimit && dailyLimit > 0

        val savedMonthAllocations = allAllocations.filter { it.monthKey == selectedMonthKey }
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMonthKey = String.format(Locale.US, "%04d-%02d", prevCal.get(Calendar.YEAR), prevCal.get(Calendar.MONTH) + 1)
        val prevMonthAllocations = allAllocations.filter { it.monthKey == prevMonthKey }

        val hasSavedAllocations = savedMonthAllocations.isNotEmpty()
        val isBudgetAllocationDraft = !hasSavedAllocations && prevMonthAllocations.isNotEmpty()

        val allocationsSourceMap = if (hasSavedAllocations) {
            savedMonthAllocations.associateBy { it.categoryId }
        } else if (prevMonthAllocations.isNotEmpty()) {
            prevMonthAllocations.associateBy { it.categoryId }
        } else {
            emptyMap()
        }

        val categoryIds = LinkedHashSet<String>().apply {
            addAll(CategoryCatalog.defaultBudgetCategories)
            addAll(allocationsSourceMap.keys)
        }

        val monthCategoryAllocations = categoryIds.map { catId ->
            val category = CategoryCatalog.getCategoryById(catId)
            val allocatedAmount = allocationsSourceMap[catId]?.allocatedAmount ?: 0.0
            val actualSpent = monthTransactions
                .filter { it.type == TransactionType.EXPENSE && it.categoryId == catId }
                .sumOf { it.amount }
            val remainingAmount = allocatedAmount - actualSpent
            val usagePercentage = if (allocatedAmount > 0) (actualSpent / allocatedAmount).toFloat() else 0f
            val isOverBudget = actualSpent > allocatedAmount && allocatedAmount > 0

            CategoryAllocationUiModel(
                category = category,
                allocatedAmount = allocatedAmount,
                actualSpent = actualSpent,
                remainingAmount = remainingAmount,
                usagePercentage = usagePercentage,
                isOverBudget = isOverBudget
            )
        }.sortedWith(
            compareByDescending<CategoryAllocationUiModel> { it.allocatedAmount > 0 }
                .thenByDescending { it.actualSpent }
        )

        val totalAllocated = monthCategoryAllocations.sumOf { it.allocatedAmount }
        val unallocatedIncome = totalIncome - totalAllocated
        val plannedShortfall = maxOf(0.0, totalAllocated - totalIncome)
        val isShortfall = totalAllocated > totalIncome && totalAllocated > 0.0

        val plannedSavingsTotal = monthCategoryAllocations
            .filter { it.category.id == "savings" || it.category.id == "emergency" }
            .sumOf { it.allocatedAmount }

        val dailyExpenseAlloc = monthCategoryAllocations.find { it.category.id == "daily_expenses" }?.allocatedAmount ?: 0.0
        val suggestedDailyLimit = if (dailyExpenseAlloc > 0 && daysInMonth > 0) dailyExpenseAlloc / daysInMonth else 0.0

        val activeLoans = allLoans.filter { it.status == LoanStatus.ACTIVE }
        val totalActiveLent = activeLoans.filter { it.type == LoanType.LENT }.sumOf { it.remainingAmount }
        val totalActiveBorrowed = activeLoans.filter { it.type == LoanType.BORROWED }.sumOf { it.remainingAmount }

        val monthOrdinaryExpenses = monthTransactions.filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" && it.categoryId != "loan_lent" }
        val totalMonthExpense = monthOrdinaryExpenses.sumOf { it.amount }
        val categoryBreakdown = if (totalMonthExpense > 0) {
            monthOrdinaryExpenses
                .groupBy { it.categoryId }
                .map { (catId, items) ->
                    val catAmount = items.sumOf { it.amount }
                    val category = CategoryCatalog.getCategoryById(catId)
                    CategorySpending(
                        category = category,
                        totalAmount = catAmount,
                        percentage = (catAmount / totalMonthExpense).toFloat(),
                        transactionCount = items.size
                    )
                }
                .sortedByDescending { it.totalAmount }
        } else emptyList()

        val monthlySpent = totalExpense
        val budgetProgress = if (monthlyLimit > 0) (monthlySpent / monthlyLimit).toFloat() else 0f
        val budgetRemaining = (monthlyLimit - monthlySpent).coerceAtLeast(0.0)

        MonthlyFinancialSummary(
            selectedMonthKey = selectedMonthKey,
            balance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netOperatingCashChange = netOperatingCashChange,
            totalActiveLent = totalActiveLent,
            totalActiveBorrowed = totalActiveBorrowed,
            monthAllocations = monthCategoryAllocations,
            isBudgetAllocationDraft = isBudgetAllocationDraft,
            totalAllocated = totalAllocated,
            unallocatedIncome = unallocatedIncome,
            plannedShortfall = plannedShortfall,
            isShortfall = isShortfall,
            plannedSavingsTotal = plannedSavingsTotal,
            todayExpenses = todayExpenses,
            dailyBudgetRemaining = dailyBudgetRemaining,
            dailyBudgetProgress = dailyBudgetProgress,
            isDailyOverBudget = isDailyOverBudget,
            suggestedDailyLimit = suggestedDailyLimit,
            monthlySpent = monthlySpent,
            budgetProgress = budgetProgress,
            budgetRemaining = budgetRemaining,
            categoryBreakdown = categoryBreakdown,
            startOfMonth = startOfMonth,
            endOfMonth = endOfMonth
        )
    }.flowOn(Dispatchers.Default)

    private val calendarGridFlow: Flow<List<CalendarDayCell>> = combine(
        _displayedMonth,
        _selectedDateTimestamp,
        repository.allTransactions
    ) { month, selectedDate, transactions ->
        generateCalendarGrid(month, selectedDate, transactions)
    }.flowOn(Dispatchers.Default)

    private data class GoalParams(
        val filterStatus: GoalStatus?,
        val detailsId: Long?,
        val isCreateOpen: Boolean,
        val editingGoal: FinancialGoalEntity?,
        val contributingGoal: FinancialGoalEntity?,
        val isAddContributionOpen: Boolean
    )

    private val goalsDataFlow: Flow<Pair<List<FinancialGoalEntity>, List<GoalContributionEntity>>> = combine(
        repository.allGoals,
        repository.allGoalContributions
    ) { goals, contributions ->
        goals to contributions
    }.flowOn(Dispatchers.Default)

    private val goalParamsFlow: Flow<GoalParams> = combine(
        listOf(
            _selectedGoalFilterStatus,
            _selectedGoalDetailsId,
            _isCreateGoalDialogOpen,
            _editingGoal,
            _contributingGoal,
            _isAddContributionDialogOpen
        )
    ) { array ->
        GoalParams(
            filterStatus = array[0] as? GoalStatus,
            detailsId = array[1] as? Long,
            isCreateOpen = array[2] as Boolean,
            editingGoal = array[3] as? FinancialGoalEntity,
            contributingGoal = array[4] as? FinancialGoalEntity,
            isAddContributionOpen = array[5] as Boolean
        )
    }.flowOn(Dispatchers.Default)

    private val archiveWrappersFlow: Flow<List<ArchiveItemWrapper>> = combine(
        repository.archivedTransactions,
        repository.archivedLoans,
        repository.archivedGoals
    ) { archivedTxs, archivedLoans, archivedGoals ->
        val now = System.currentTimeMillis()
        val txWrappers = archivedTxs.map { entity ->
            val calArchive = Calendar.getInstance().apply {
                timeInMillis = entity.archivedAt ?: now
                add(Calendar.MONTH, 2)
            }
            val daysRemaining = ((calArchive.timeInMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()
            val retentionText = if (daysRemaining <= 0) "Eligible for auto-purge" else "Permanent deletion in $daysRemaining days"
            ArchiveItemWrapper.Transaction(
                entity = entity,
                daysRemaining = daysRemaining,
                formattedRetentionRemaining = retentionText
            )
        }

        val loanWrappers = archivedLoans.map { entity ->
            val calArchive = Calendar.getInstance().apply {
                timeInMillis = entity.archivedAt ?: now
                add(Calendar.MONTH, 2)
            }
            val daysRemaining = ((calArchive.timeInMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()
            val retentionText = if (daysRemaining <= 0) "Eligible for auto-purge" else "Permanent deletion in $daysRemaining days"
            ArchiveItemWrapper.Loan(
                entity = entity,
                daysRemaining = daysRemaining,
                formattedRetentionRemaining = retentionText
            )
        }

        val goalWrappers = archivedGoals.map { entity ->
            val calArchive = Calendar.getInstance().apply {
                timeInMillis = entity.archivedAt ?: now
                add(Calendar.MONTH, 2)
            }
            val daysRemaining = ((calArchive.timeInMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()
            val retentionText = if (daysRemaining <= 0) "Eligible for auto-purge" else "Permanent deletion in $daysRemaining days"
            ArchiveItemWrapper.Goal(
                entity = entity,
                daysRemaining = daysRemaining,
                formattedRetentionRemaining = retentionText
            )
        }

        (txWrappers + loanWrappers + goalWrappers).sortedByDescending { it.archivedAt }
    }.flowOn(Dispatchers.Default)

    private data class IntermediateData(
        val coreData: CoreData,
        val monthlySummary: MonthlyFinancialSummary,
        val calendarDays: List<CalendarDayCell>,
        val allArchivedItems: List<ArchiveItemWrapper>,
        val goalsPair: Pair<List<FinancialGoalEntity>, List<GoalContributionEntity>>
    )

    private val intermediateDataFlow: Flow<IntermediateData> = combine(
        coreDataFlow,
        monthlySummaryFlow,
        calendarGridFlow,
        archiveWrappersFlow,
        goalsDataFlow
    ) { coreData, monthlySummary, calendarDays, allArchivedItems, goalsPair ->
        IntermediateData(coreData, monthlySummary, calendarDays, allArchivedItems, goalsPair)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<FinanceUiState> = combine(
        intermediateDataFlow,
        calendarParamsFlow,
        filterParamsFlow,
        archiveParamsFlow,
        dialogParamsFlow
    ) { intermediate, calendarParams, filterParams, archiveParams, dialogParams ->
        val coreData = intermediate.coreData
        val monthlySummary = intermediate.monthlySummary
        val allTransactions = coreData.allTransactions
        val allLoans = coreData.allLoans
        val calendarDays = intermediate.calendarDays
        val allArchivedItems = intermediate.allArchivedItems

        val startOfMonth = monthlySummary.startOfMonth
        val endOfMonth = monthlySummary.endOfMonth

        val selectedDayStart = DateUtils.getStartOfDay(calendarParams.selectedDate)
        val selectedDayEnd = DateUtils.getEndOfDay(calendarParams.selectedDate)

        val selectedDayTransactions = allTransactions.filter {
            it.timestamp in selectedDayStart..selectedDayEnd
        }.sortedByDescending { it.timestamp }

        val dayIncome = selectedDayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val dayExpense = selectedDayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val dayLoan = selectedDayTransactions.filter { it.type == TransactionType.LOAN }.sumOf { it.amount }

        val selectedDaySummary = DaySummaryStats(
            dateTimestamp = calendarParams.selectedDate,
            formattedDate = DateUtils.formatHeaderDate(calendarParams.selectedDate),
            totalIncome = dayIncome,
            totalExpense = dayExpense,
            totalLoan = dayLoan,
            netBalance = dayIncome - dayExpense
        )

        val search = filterParams.search
        val typeFilter = filterParams.type
        val categoryFilter = filterParams.categoryId
        val dateFilter = filterParams.dateOption

        val filteredTransactions = allTransactions.filter { item ->
            val matchesType = when (typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.INCOME -> item.type == TransactionType.INCOME
                TransactionTypeFilter.EXPENSE -> item.type == TransactionType.EXPENSE
                TransactionTypeFilter.LOAN -> item.type == TransactionType.LOAN
            }
            val matchesCategory = categoryFilter == null || item.categoryId == categoryFilter
            val matchesDate = when (dateFilter) {
                DateFilterOption.ALL_TIME -> true
                DateFilterOption.THIS_MONTH -> item.timestamp in startOfMonth..endOfMonth
                DateFilterOption.THIS_WEEK -> item.timestamp >= DateUtils.getStartOfWeek()
                DateFilterOption.TODAY -> item.timestamp >= DateUtils.getStartOfDay()
            }
            val matchesSearch = if (search.isBlank()) true else {
                val q = search.trim().lowercase()
                item.note.lowercase().contains(q) ||
                        item.categoryName.lowercase().contains(q) ||
                        item.amount.toString().contains(q)
            }
            matchesType && matchesCategory && matchesDate && matchesSearch
        }

        val loanTypeFilter = filterParams.loanType
        val loanStatusFilter = filterParams.loanStatus

        val filteredLoans = allLoans.filter { loan ->
            val matchesType = when (loanTypeFilter) {
                LoanTypeFilter.ALL -> true
                LoanTypeFilter.LENT -> loan.type == LoanType.LENT
                LoanTypeFilter.BORROWED -> loan.type == LoanType.BORROWED
            }
            val matchesStatus = when (loanStatusFilter) {
                LoanStatusFilter.ALL -> true
                LoanStatusFilter.ACTIVE -> loan.status == LoanStatus.ACTIVE
                LoanStatusFilter.SETTLED -> loan.status == LoanStatus.SETTLED
            }
            matchesType && matchesStatus
        }

        val filteredArchivedItems = allArchivedItems.filter { item ->
            val matchesType = when (archiveParams.filterType) {
                ArchiveFilterType.ALL -> true
                ArchiveFilterType.INCOME -> item is ArchiveItemWrapper.Transaction && item.entity.type == TransactionType.INCOME
                ArchiveFilterType.EXPENSE -> item is ArchiveItemWrapper.Transaction && item.entity.type == TransactionType.EXPENSE
                ArchiveFilterType.LOANS -> item is ArchiveItemWrapper.Loan
                ArchiveFilterType.GOALS -> item is ArchiveItemWrapper.Goal
            }
            val matchesQuery = if (archiveParams.searchQuery.isBlank()) true else {
                val q = archiveParams.searchQuery.trim().lowercase()
                item.title.lowercase().contains(q) || item.amount.toString().contains(q)
            }
            matchesType && matchesQuery
        }

        // Financial Goals processing
        val (rawGoals, rawContributions) = intermediate.goalsPair
        val activeRawGoals = rawGoals.filter { it.archivedAt == null }

        val allGoalUiModels = activeRawGoals.map { goal ->
            val contributionsForGoal = rawContributions.filter { it.goalId == goal.id && it.archivedAt == null }
                .sortedByDescending { it.contributionDate }
            val savedFromContributions = contributionsForGoal.sumOf { it.amount }
            val totalSaved = goal.initialSavedAmount + savedFromContributions
            val remainingAmount = maxOf(0.0, goal.targetAmount - totalSaved)
            val progress = if (goal.targetAmount > 0) (totalSaved / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
            val progressPercent = if (goal.targetAmount > 0) ((totalSaved / goal.targetAmount) * 100).coerceAtLeast(0.0) else 0.0
            val isCompleted = totalSaved >= goal.targetAmount || goal.status == GoalStatus.COMPLETED

            val isOverdue: Boolean
            val remainingMonths: Int?
            val requiredMonthly: Double?

            if (goal.targetDate != null && !isCompleted) {
                val nowCal = calendarParams.month.clone() as Calendar
                val targetCal = Calendar.getInstance().apply { timeInMillis = goal.targetDate }
                val yearDiff = targetCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)
                val monthDiff = targetCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH)
                val totalMonthsDiff = yearDiff * 12 + monthDiff
                if (totalMonthsDiff < 0) {
                    isOverdue = true
                    remainingMonths = 0
                    requiredMonthly = null
                } else if (totalMonthsDiff == 0) {
                    isOverdue = false
                    remainingMonths = 1
                    requiredMonthly = remainingAmount
                } else {
                    isOverdue = false
                    val months = totalMonthsDiff + 1
                    remainingMonths = months
                    requiredMonthly = if (remainingAmount > 0) remainingAmount / months else 0.0
                }
            } else {
                isOverdue = false
                remainingMonths = null
                requiredMonthly = if (!isCompleted) goal.targetMonthlyContribution else null
            }

            val thisMonthSaved = contributionsForGoal
                .filter { it.contributionDate in startOfMonth..endOfMonth }
                .sumOf { it.amount }

            FinancialGoalUiModel(
                goal = goal,
                totalSaved = totalSaved,
                remainingAmount = remainingAmount,
                progress = progress,
                progressPercent = progressPercent,
                isCompleted = isCompleted,
                isOverdue = isOverdue,
                remainingMonths = remainingMonths,
                requiredMonthlySaving = requiredMonthly,
                thisMonthSaved = thisMonthSaved,
                contributions = contributionsForGoal
            )
        }

        val activeGoalModels = allGoalUiModels.filter { it.goal.status == GoalStatus.ACTIVE }
        val totalGoalTarget = activeGoalModels.sumOf { it.goal.targetAmount }
        val totalGoalSaved = activeGoalModels.sumOf { it.totalSaved }
        val overallGoalProgressPercent = if (totalGoalTarget > 0) ((totalGoalSaved / totalGoalTarget) * 100).coerceIn(0.0, 100.0) else 0.0
        val thisMonthGoalSaved = rawContributions.filter { it.archivedAt == null && it.contributionDate in startOfMonth..endOfMonth }.sumOf { it.amount }
        val totalMonthlyGoalRequired = activeGoalModels.filter { !it.isCompleted }.sumOf { it.requiredMonthlySaving ?: it.goal.targetMonthlyContribution ?: 0.0 }

        val nonSavingsAllocated = monthlySummary.monthAllocations
            .filter { it.category.id != "savings" && it.category.id != "emergency" }
            .sumOf { it.allocatedAmount }

        val monthlyGoalSavingCapacity = if (monthlySummary.totalAllocated > 0) {
            maxOf(0.0, monthlySummary.totalIncome - nonSavingsAllocated)
        } else {
            maxOf(0.0, monthlySummary.totalIncome - monthlySummary.totalExpense)
        }

        val isGoalCapacityDeficit = totalMonthlyGoalRequired > monthlyGoalSavingCapacity && totalMonthlyGoalRequired > 0.0
        val goalCapacityDifference = monthlyGoalSavingCapacity - totalMonthlyGoalRequired

        FinanceUiState(
            activeTab = coreData.activeTab,
            moreSubDestination = archiveParams.moreDest,
            displayedMonth = calendarParams.month,
            selectedDateTimestamp = calendarParams.selectedDate,
            calendarDays = calendarDays,
            selectedDayTransactions = selectedDayTransactions,
            selectedDaySummary = selectedDaySummary,
            isDayDetailSheetOpen = calendarParams.isSheetOpen,
            selectedMonthKey = monthlySummary.selectedMonthKey,
            balance = monthlySummary.balance,
            totalIncome = monthlySummary.totalIncome,
            totalExpense = monthlySummary.totalExpense,
            netOperatingCashChange = monthlySummary.netOperatingCashChange,
            totalActiveLent = monthlySummary.totalActiveLent,
            totalActiveBorrowed = monthlySummary.totalActiveBorrowed,
            monthAllocations = monthlySummary.monthAllocations,
            isBudgetAllocationDraft = monthlySummary.isBudgetAllocationDraft,
            totalAllocated = monthlySummary.totalAllocated,
            unallocatedIncome = monthlySummary.unallocatedIncome,
            plannedShortfall = monthlySummary.plannedShortfall,
            isShortfall = monthlySummary.isShortfall,
            plannedSavingsTotal = monthlySummary.plannedSavingsTotal,
            todayExpenses = monthlySummary.todayExpenses,
            dailyBudgetLimit = coreData.dailyLimit,
            dailyBudgetRemaining = monthlySummary.dailyBudgetRemaining,
            dailyBudgetProgress = monthlySummary.dailyBudgetProgress,
            isDailyOverBudget = monthlySummary.isDailyOverBudget,
            suggestedDailyLimit = monthlySummary.suggestedDailyLimit,
            monthlyLimit = coreData.monthlyLimit,
            monthlySpent = monthlySummary.monthlySpent,
            budgetProgress = monthlySummary.budgetProgress,
            budgetRemaining = monthlySummary.budgetRemaining,
            allTransactions = allTransactions,
            filteredTransactions = filteredTransactions,
            categorySpendBreakdown = monthlySummary.categoryBreakdown,
            searchQuery = search,
            selectedTypeFilter = typeFilter,
            selectedCategoryFilterId = categoryFilter,
            selectedDateFilter = dateFilter,
            allLoans = allLoans,
            filteredLoans = filteredLoans,
            selectedLoanFilter = loanTypeFilter,
            selectedLoanStatusFilter = loanStatusFilter,
            repayingLoan = dialogParams.repayingLoan,
            isRepayDialogOpen = dialogParams.isRepayDialogOpen,
            isRepaymentSaving = dialogParams.isRepaymentSaving,
            loanOperationError = dialogParams.loanOperationError,
            allGoals = allGoalUiModels,
            activeGoals = activeGoalModels,
            totalGoalTarget = totalGoalTarget,
            totalGoalSaved = totalGoalSaved,
            overallGoalProgressPercent = overallGoalProgressPercent,
            thisMonthGoalSaved = thisMonthGoalSaved,
            totalMonthlyGoalRequired = totalMonthlyGoalRequired,
            monthlyGoalSavingCapacity = monthlyGoalSavingCapacity,
            isGoalCapacityDeficit = isGoalCapacityDeficit,
            goalCapacityDifference = goalCapacityDifference,
            archivedItems = allArchivedItems,
            filteredArchivedItems = filteredArchivedItems,
            archiveFilterType = archiveParams.filterType,
            archiveSearchQuery = archiveParams.searchQuery,
            itemToPermanentlyDelete = archiveParams.itemToDelete,
            recentlyArchivedNote = archiveParams.recentlyArchivedNote,
            isAddTransactionSheetOpen = dialogParams.isAddTransactionSheetOpen,
            editingTransaction = dialogParams.editingTransaction,
            defaultEntryType = dialogParams.defaultEntryType,
            defaultLoanType = dialogParams.defaultLoanType,
            isBudgetLimitDialogOpen = dialogParams.isBudgetLimitDialogOpen,
            isBudgetPlanningDialogOpen = dialogParams.isBudgetPlanningDialogOpen,
            isDailyLimitDialogOpen = dialogParams.isDailyLimitDialogOpen,
            currencySymbol = "৳"
        )
    }.combine(goalParamsFlow) { state, goalParams ->
        val filteredGoals = if (goalParams.filterStatus != null) {
            state.allGoals.filter { it.goal.status == goalParams.filterStatus }
        } else {
            state.allGoals
        }
        state.copy(
            allGoals = filteredGoals,
            selectedGoalFilterStatus = goalParams.filterStatus,
            selectedGoalDetailsId = goalParams.detailsId,
            isCreateGoalDialogOpen = goalParams.isCreateOpen,
            editingGoal = goalParams.editingGoal,
            contributingGoal = goalParams.contributingGoal,
            isAddContributionDialogOpen = goalParams.isAddContributionOpen
        )
    }.combine(backupNotificationParamsFlow) { state, backupParams ->
        state.copy(
            isExportingBackup = backupParams.isExportingBackup,
            isRestoringBackup = backupParams.isRestoringBackup,
            backupPreview = backupParams.backupPreview,
            pendingRestoreData = backupParams.pendingRestoreData,
            backupOperationMessage = backupParams.backupOperationMessage,
            backupOperationError = backupParams.backupOperationError,
            notificationPreferences = backupParams.notificationPreferences,
            notificationPermissionGranted = backupParams.notificationPermissionGranted
        )
    }.combine(pdfParamsFlow) { state, pdfParams ->
        state.copy(
            pdfFilterState = pdfParams.filterState,
            isGeneratingPdf = pdfParams.isGenerating,
            pdfExportMessage = pdfParams.message,
            pdfExportError = pdfParams.error,
            pdfExportFileUri = pdfParams.fileUri
        )
    }.combine(purchasePlannerParamsFlow) { state, purchaseParams ->
        val allItems = purchaseParams.items
        val pendingCount = allItems.count { !it.isCompleted }
        val completedCount = allItems.count { it.isCompleted }

        val q = purchaseParams.searchQuery.trim().lowercase()
        val filtered = allItems.filter { item ->
            val matchesFilter = when (purchaseParams.filter) {
                PurchaseItemFilter.ALL -> true
                PurchaseItemFilter.PENDING -> !item.isCompleted
                PurchaseItemFilter.COMPLETED -> item.isCompleted
            }
            val matchesSearch = if (q.isBlank()) true else {
                item.title.lowercase().contains(q) || item.note.lowercase().contains(q)
            }
            matchesFilter && matchesSearch
        }.sortedWith(
            compareBy<PurchaseItemEntity> { it.isCompleted }
                .thenByDescending { if (it.isCompleted) (it.completedAt ?: it.createdAt) else it.createdAt }
        )

        state.copy(
            purchaseItems = allItems,
            filteredPurchaseItems = filtered,
            purchaseItemFilter = purchaseParams.filter,
            purchaseSearchQuery = purchaseParams.searchQuery,
            pendingPurchaseCount = pendingCount,
            completedPurchaseCount = completedCount,
            isAddPurchaseItemDialogOpen = purchaseParams.isAddDialogOpen,
            editingPurchaseItem = purchaseParams.editingItem,
            itemToDeletePurchase = purchaseParams.itemToDelete
        )
    }.combine(smsImportParamsFlow) { state, smsParams ->
        state.copy(
            isSmsImportEnabled = smsParams.prefs.isEnabled,
            smsImportActivatedAt = smsParams.prefs.activatedAt,
            smsImportLastAt = smsParams.prefs.lastImportedAt,
            smsImportedCount = smsParams.prefs.importedCount,
            smsAutoPostUnambiguous = smsParams.prefs.autoPostUnambiguous,
            importedSmsRecords = smsParams.records,
            pendingReviewSmsRecords = smsParams.records.filter { it.status == com.engrshuvo.financemanager.data.model.SmsImportStatus.PENDING_REVIEW },
            smsRecordToReview = smsParams.recordToReview,
            smsPasteInput = smsParams.pasteInput,
            smsPasteTestResult = smsParams.pasteTestResult,
            smsOperationMessage = smsParams.operationMessage
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    private fun generateCalendarGrid(
        calendarMonth: Calendar,
        selectedDateTs: Long,
        transactions: List<TransactionEntity>
    ): List<CalendarDayCell> {
        val cells = ArrayList<CalendarDayCell>(42)
        val cal = (calendarMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadDays = firstDayOfWeek - Calendar.SUNDAY

        cal.add(Calendar.DAY_OF_MONTH, -leadDays)

        val todayCal = Calendar.getInstance()
        val totalCells = 42

        for (i in 0 until totalCells) {
            val cellTs = cal.timeInMillis
            val isCurMonth = cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
            val isToday = DateUtils.isSameDay(cal, todayCal)
            val isSelected = DateUtils.isSameDay(cellTs, selectedDateTs)

            val dayStart = cellTs
            val dayEnd = cellTs + (24 * 60 * 60 * 1000) - 1

            var hasIncome = false
            var hasExpense = false
            var hasLoan = false
            var incomeSum = 0.0
            var expenseSum = 0.0
            var loanSum = 0.0
            var count = 0

            for (tx in transactions) {
                if (tx.timestamp in dayStart..dayEnd) {
                    count++
                    when (tx.type) {
                        TransactionType.INCOME -> {
                            hasIncome = true
                            incomeSum += tx.amount
                        }
                        TransactionType.EXPENSE -> {
                            hasExpense = true
                            expenseSum += tx.amount
                        }
                        TransactionType.LOAN -> {
                            hasLoan = true
                            loanSum += tx.amount
                        }
                    }
                }
            }

            cells.add(
                CalendarDayCell(
                    timestamp = cellTs,
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = isCurMonth,
                    isToday = isToday,
                    isSelected = isSelected,
                    hasIncome = hasIncome,
                    hasExpense = hasExpense,
                    hasLoan = hasLoan,
                    dayIncomeTotal = incomeSum,
                    dayExpenseTotal = expenseSum,
                    dayLoanTotal = loanSum,
                    transactionCount = count
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return cells
    }

    fun selectTab(tab: FinanceTab) {
        _activeTab.value = tab
    }

    fun changeMonth(offset: Int) {
        val cal = (_displayedMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, offset)
        }
        _displayedMonth.value = cal
    }

    fun resetToCurrentMonth() {
        _displayedMonth.value = Calendar.getInstance()
        _selectedDateTimestamp.value = System.currentTimeMillis()
    }

    fun selectDate(timestamp: Long, openSheet: Boolean = true) {
        _selectedDateTimestamp.value = timestamp
        if (openSheet) {
            _isDayDetailSheetOpen.value = true
        }
    }

    fun closeDayDetailSheet() {
        _isDayDetailSheetOpen.value = false
    }

    fun openAddTransactionSheet(
        type: TransactionType = TransactionType.EXPENSE,
        loanType: LoanType = LoanType.LENT
    ) {
        _transactionSheetState.value = TransactionSheetState(
            isOpen = true,
            editingTransaction = null,
            defaultEntryType = type,
            defaultLoanType = loanType
        )
    }

    fun openEditTransactionSheet(transaction: TransactionEntity) {
        _transactionSheetState.value = TransactionSheetState(
            isOpen = true,
            editingTransaction = transaction,
            defaultEntryType = transaction.type,
            defaultLoanType = LoanType.LENT
        )
    }

    fun closeAddTransactionSheet() {
        _transactionSheetState.value = TransactionSheetState(
            isOpen = false,
            editingTransaction = null,
            defaultEntryType = TransactionType.EXPENSE,
            defaultLoanType = LoanType.LENT
        )
    }

    fun saveTransaction(
        id: Long = 0,
        type: TransactionType,
        amount: Double,
        categoryId: String,
        categoryName: String,
        note: String,
        timestamp: Long
    ) {
        if (!isTransactionSaving.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch {
            try {
                val entity = TransactionEntity(
                    id = id,
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    note = note.trim(),
                    timestamp = timestamp
                )
                if (id == 0L) {
                    repository.insertTransaction(entity)
                } else {
                    repository.updateTransaction(entity)
                }
                closeAddTransactionSheet()
            } finally {
                isTransactionSaving.set(false)
            }
        }
    }

    fun saveLoanTransaction(
        type: LoanType,
        personName: String,
        phoneNumber: String,
        amount: Double,
        startDate: Long,
        dueDate: Long?,
        note: String
    ) {
        if (!isTransactionSaving.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch {
            try {
                val loan = LoanEntity(
                    id = 0,
                    type = type,
                    personName = personName.trim(),
                    phoneNumber = phoneNumber.trim(),
                    initialAmount = amount,
                    remainingAmount = amount,
                    status = LoanStatus.ACTIVE,
                    startDate = startDate,
                    dueDate = dueDate,
                    note = note.trim()
                )
                repository.insertLoanWithTransaction(loan)
                closeAddTransactionSheet()
            } finally {
                isTransactionSaving.set(false)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    fun openRepayDialog(loan: LoanEntity) {
        _repayingLoan.value = loan
        _loanOperationError.value = null
        _isRepayDialogOpen.value = true
    }

    fun closeRepayDialog() {
        _isRepayDialogOpen.value = false
        _repayingLoan.value = null
        _loanOperationError.value = null
    }

    fun clearLoanOperationError() {
        _loanOperationError.value = null
    }

    fun submitLoanRepayment(loanId: Long, amount: Double, note: String) {
        if (!isRepaymentSaving.compareAndSet(false, true)) {
            return
        }
        _isRepaymentSavingState.value = true
        _loanOperationError.value = null
        viewModelScope.launch {
            try {
                val success = repository.recordLoanRepayment(loanId, amount, note)
                if (success) {
                    closeRepayDialog()
                } else {
                    _loanOperationError.value = "Failed to record repayment. Amount cannot exceed remaining balance."
                }
            } catch (e: Exception) {
                _loanOperationError.value = "Failed to record repayment: ${e.message ?: "Database error"}"
            } finally {
                isRepaymentSaving.set(false)
                _isRepaymentSavingState.value = false
            }
        }
    }

    fun settleLoanWithCash(loanId: Long) {
        viewModelScope.launch {
            try {
                val success = repository.settleLoanWithCash(loanId)
                if (!success) {
                    _loanOperationError.value = "Failed to record cash settlement."
                }
            } catch (e: Exception) {
                _loanOperationError.value = "Error settling loan: ${e.message ?: "Database error"}"
            }
        }
    }

    fun settleLoanWithoutCash(loanId: Long, reason: String = "Forgiven / Written off") {
        viewModelScope.launch {
            try {
                val success = repository.settleLoanWithoutCash(loanId, reason)
                if (!success) {
                    _loanOperationError.value = "Failed to settle loan."
                }
            } catch (e: Exception) {
                _loanOperationError.value = "Error settling loan: ${e.message ?: "Database error"}"
            }
        }
    }

    fun markLoanSettled(loanId: Long) {
        settleLoanWithoutCash(loanId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: TransactionTypeFilter) {
        _selectedTypeFilter.value = filter
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryFilterId.value = categoryId
    }

    fun setDateFilter(dateFilter: DateFilterOption) {
        _selectedDateFilter.value = dateFilter
    }

    fun setLoanTypeFilter(filter: LoanTypeFilter) {
        _selectedLoanFilter.value = filter
    }

    fun setLoanStatusFilter(filter: LoanStatusFilter) {
        _selectedLoanStatusFilter.value = filter
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedTypeFilter.value = TransactionTypeFilter.ALL
        _selectedCategoryFilterId.value = null
        _selectedDateFilter.value = DateFilterOption.ALL_TIME
        _selectedLoanFilter.value = LoanTypeFilter.ALL
        _selectedLoanStatusFilter.value = LoanStatusFilter.ACTIVE
    }

    fun openBudgetLimitDialog() {
        _isBudgetLimitDialogOpen.value = true
    }

    fun closeBudgetLimitDialog() {
        _isBudgetLimitDialogOpen.value = false
    }

    fun updateMonthlyBudgetLimit(limit: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudgetLimit(limit)
            closeBudgetLimitDialog()
        }
    }

    fun openBudgetPlanningDialog() {
        _isBudgetPlanningDialogOpen.value = true
    }

    fun closeBudgetPlanningDialog() {
        _isBudgetPlanningDialogOpen.value = false
    }

    fun openDailyLimitDialog() {
        _isDailyLimitDialogOpen.value = true
    }

    fun closeDailyLimitDialog() {
        _isDailyLimitDialogOpen.value = false
    }

    fun saveCategoryAllocations(allocations: Map<String, Double>) {
        viewModelScope.launch {
            val cal = _displayedMonth.value
            val monthKey = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            val now = System.currentTimeMillis()
            val entities = allocations.map { (catId, amount) ->
                val cat = CategoryCatalog.getCategoryById(catId)
                BudgetAllocationEntity(
                    monthKey = monthKey,
                    categoryId = catId,
                    categoryName = cat.name,
                    allocatedAmount = amount.coerceAtLeast(0.0),
                    updatedAt = now
                )
            }
            repository.saveBudgetAllocations(entities)
            closeBudgetPlanningDialog()
        }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val cal = _displayedMonth.value
            val toMonthKey = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val fromMonthKey = String.format(Locale.US, "%04d-%02d", prevCal.get(Calendar.YEAR), prevCal.get(Calendar.MONTH) + 1)
            repository.copyAllocations(fromMonthKey, toMonthKey)
        }
    }

    fun updateDailyBudgetLimit(limit: Double) {
        viewModelScope.launch {
            repository.setDailyBudgetLimit(limit.coerceAtLeast(0.0))
            closeDailyLimitDialog()
        }
    }

    fun setActiveTab(tab: FinanceTab) {
        _activeTab.value = tab
        _moreSubDestination.value = MoreSubDestination.NONE
    }

    fun navigateToMoreSubDestination(dest: MoreSubDestination) {
        _moreSubDestination.value = dest
        if (dest == MoreSubDestination.ARCHIVE) {
            viewModelScope.launch {
                repository.purgeExpiredArchivedRecords()
            }
        }
    }

    fun navigateBackFromMoreSubDestination() {
        _moreSubDestination.value = MoreSubDestination.NONE
    }

    fun restoreTransaction(id: Long) {
        viewModelScope.launch {
            repository.restoreTransaction(id)
        }
    }

    fun restoreLoan(loanId: Long) {
        viewModelScope.launch {
            repository.restoreLoan(loanId)
        }
    }

    fun openPermanentDeleteDialog(item: ArchiveItemWrapper) {
        _itemToPermanentlyDelete.value = item
    }

    fun closePermanentDeleteDialog() {
        _itemToPermanentlyDelete.value = null
    }

    fun confirmPermanentDelete() {
        val item = _itemToPermanentlyDelete.value ?: return
        viewModelScope.launch {
            when (item) {
                is ArchiveItemWrapper.Transaction -> repository.permanentlyDeleteTransaction(item.id)
                is ArchiveItemWrapper.Loan -> repository.permanentlyDeleteLoan(item.id)
                is ArchiveItemWrapper.Goal -> repository.permanentlyDeleteGoal(item.id)
            }
            closePermanentDeleteDialog()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Financial Goals Actions
    // ---------------------------------------------------------------------------------------------

    fun openCreateGoalDialog() {
        _editingGoal.value = null
        _isCreateGoalDialogOpen.value = true
    }

    fun openEditGoalDialog(goal: FinancialGoalEntity) {
        _editingGoal.value = goal
        _isCreateGoalDialogOpen.value = true
    }

    fun dismissGoalDialog() {
        _isCreateGoalDialogOpen.value = false
        _editingGoal.value = null
    }

    fun saveGoal(
        name: String,
        category: GoalCategory,
        targetAmount: Double,
        initialSavedAmount: Double,
        targetDate: Long?,
        priority: GoalPriority,
        targetMonthlyContribution: Double?
    ) {
        if (isGoalSaving.compareAndSet(false, true)) {
            viewModelScope.launch {
                try {
                    val currentEditing = _editingGoal.value
                    if (currentEditing != null) {
                        repository.updateGoal(
                            currentEditing.copy(
                                name = name.trim(),
                                category = category,
                                targetAmount = targetAmount,
                                initialSavedAmount = initialSavedAmount,
                                targetDate = targetDate,
                                priority = priority,
                                targetMonthlyContribution = targetMonthlyContribution,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        repository.insertGoal(
                            FinancialGoalEntity(
                                name = name.trim(),
                                category = category,
                                targetAmount = targetAmount,
                                initialSavedAmount = initialSavedAmount,
                                targetDate = targetDate,
                                priority = priority,
                                status = GoalStatus.ACTIVE,
                                targetMonthlyContribution = targetMonthlyContribution
                            )
                        )
                    }
                    _isCreateGoalDialogOpen.value = false
                    _editingGoal.value = null
                } finally {
                    isGoalSaving.set(false)
                }
            }
        }
    }

    fun updateGoalStatus(goal: FinancialGoalEntity, newStatus: GoalStatus) {
        viewModelScope.launch {
            repository.updateGoal(goal.copy(status = newStatus))
        }
    }

    fun archiveGoal(goalId: Long) {
        viewModelScope.launch {
            repository.archiveGoal(goalId)
            if (_selectedGoalDetailsId.value == goalId) {
                _selectedGoalDetailsId.value = null
            }
        }
    }

    fun restoreGoal(goalId: Long) {
        viewModelScope.launch {
            repository.restoreGoal(goalId)
        }
    }

    fun permanentlyDeleteGoal(goalId: Long) {
        viewModelScope.launch {
            repository.permanentlyDeleteGoal(goalId)
            if (_selectedGoalDetailsId.value == goalId) {
                _selectedGoalDetailsId.value = null
            }
        }
    }

    fun selectGoalDetails(goalId: Long?) {
        _selectedGoalDetailsId.value = goalId
    }

    fun openAddContributionDialog(goal: FinancialGoalEntity) {
        _contributingGoal.value = goal
        _isAddContributionDialogOpen.value = true
    }

    fun dismissAddContributionDialog() {
        _isAddContributionDialogOpen.value = false
        _contributingGoal.value = null
    }

    fun saveGoalContribution(
        goalId: Long,
        amount: Double,
        date: Long,
        note: String
    ) {
        if (isContributionSaving.compareAndSet(false, true)) {
            viewModelScope.launch {
                try {
                    repository.addGoalContribution(
                        GoalContributionEntity(
                            goalId = goalId,
                            amount = amount,
                            contributionDate = date,
                            note = note.trim()
                        )
                    )
                    _isAddContributionDialogOpen.value = false
                    _contributingGoal.value = null
                } finally {
                    isContributionSaving.set(false)
                }
            }
        }
    }

    fun deleteGoalContribution(contributionId: Long) {
        viewModelScope.launch {
            repository.deleteGoalContribution(contributionId)
        }
    }

    fun setGoalFilterStatus(status: GoalStatus?) {
        _selectedGoalFilterStatus.value = status
    }

    fun setArchiveFilterType(filterType: ArchiveFilterType) {
        _archiveFilterType.value = filterType
    }

    fun setArchiveSearchQuery(query: String) {
        _archiveSearchQuery.value = query
    }

    fun purgeExpiredArchivedRecords() {
        viewModelScope.launch {
            repository.purgeExpiredArchivedRecords()
        }
    }

    fun updateMorningNotificationEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            notificationPreferencesRepo?.updateMorningNotificationEnabled(enabled)
            val updated = _notificationPrefs.value.copy(morningNotificationEnabled = enabled)
            _notificationPrefs.value = updated
            NotificationScheduler.scheduleMorningBudgetWork(context, updated)
        }
    }

    fun updateMorningNotificationTime(hour: Int, minute: Int, context: Context) {
        viewModelScope.launch {
            notificationPreferencesRepo?.updateMorningNotificationTime(hour, minute)
            val updated = _notificationPrefs.value.copy(morningNotificationHour = hour, morningNotificationMinute = minute)
            _notificationPrefs.value = updated
            NotificationScheduler.scheduleMorningBudgetWork(context, updated)
        }
    }

    fun updateExpenseReminderEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            notificationPreferencesRepo?.updateExpenseReminderEnabled(enabled)
            val updated = _notificationPrefs.value.copy(expenseReminderEnabled = enabled)
            _notificationPrefs.value = updated
            NotificationScheduler.scheduleExpenseReminderWork(context, updated)
        }
    }

    fun updateExpenseReminderInterval(hours: Int, context: Context) {
        viewModelScope.launch {
            notificationPreferencesRepo?.updateExpenseReminderInterval(hours)
            val updated = _notificationPrefs.value.copy(expenseReminderIntervalHours = hours)
            _notificationPrefs.value = updated
            NotificationScheduler.scheduleExpenseReminderWork(context, updated)
        }
    }

    fun updateQuietHours(enabled: Boolean, startHour: Int, endHour: Int, context: Context) {
        viewModelScope.launch {
            notificationPreferencesRepo?.updateQuietHours(enabled, startHour, endHour)
            val updated = _notificationPrefs.value.copy(
                quietHoursEnabled = enabled,
                quietHoursStartHour = startHour,
                quietHoursEndHour = endHour
            )
            _notificationPrefs.value = updated
            NotificationScheduler.scheduleExpenseReminderWork(context, updated)
        }
    }

    fun setNotificationPermissionStatus(granted: Boolean) {
        _notificationPermissionGranted.value = granted
    }

    fun exportBackupToUri(uri: android.net.Uri, context: Context) {
        if (_isExportingBackup.value || _isRestoringBackup.value) return
        viewModelScope.launch {
            _isExportingBackup.value = true
            try {
                val backupData = repository.exportBackupData()
                val jsonString = backupData.toJsonString()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    } ?: throw IllegalStateException("Unable to open output stream for selected file.")
                }
                _backupOperationMessage.value = "Backup exported successfully (${backupData.transactions.size} transactions, ${backupData.loans.size} loans)."
            } catch (e: Exception) {
                _backupOperationError.value = "Export failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isExportingBackup.value = false
            }
        }
    }

    fun readBackupFile(uri: android.net.Uri, context: Context) {
        if (_isRestoringBackup.value || _isExportingBackup.value) return
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } ?: throw IllegalStateException("Unable to open input stream for selected file.")
                }
                val backupData = com.engrshuvo.financemanager.data.model.FinanceBackupData.fromJsonString(jsonString)
                _pendingRestoreData.value = backupData
                _backupPreview.value = backupData.getSummaryPreview()
            } catch (e: Exception) {
                _backupOperationError.value = "Validation error: ${e.message ?: "Invalid backup file"}"
                _backupPreview.value = null
                _pendingRestoreData.value = null
            }
        }
    }

    fun confirmRestoreBackup(context: Context) {
        val backupData = _pendingRestoreData.value ?: return
        if (_isRestoringBackup.value || _isExportingBackup.value) return
        viewModelScope.launch {
            _isRestoringBackup.value = true
            try {
                val result = repository.restoreBackupData(backupData)
                if (result.isSuccess) {
                    _backupOperationMessage.value = "Successfully restored ${result.getOrNull() ?: 0} records."
                    _backupPreview.value = null
                    _pendingRestoreData.value = null
                } else {
                    _backupOperationError.value = "Restore failed: ${result.exceptionOrNull()?.message ?: "Transaction rolled back"}"
                }
            } catch (e: Exception) {
                _backupOperationError.value = "Restore failed: ${e.message ?: "Transaction rolled back"}"
            } finally {
                _isRestoringBackup.value = false
            }
        }
    }

    fun dismissRestorePreview() {
        _backupPreview.value = null
        _pendingRestoreData.value = null
    }

    fun dismissBackupOperationMessage() {
        _backupOperationMessage.value = null
        _backupOperationError.value = null
    }

    // PDF Export Center Actions
    fun updatePdfFilterState(transform: (com.engrshuvo.financemanager.data.model.PdfExportFilterState) -> com.engrshuvo.financemanager.data.model.PdfExportFilterState) {
        _pdfFilterState.value = transform(_pdfFilterState.value)
    }

    fun resetPdfFilterState() {
        _pdfFilterState.value = com.engrshuvo.financemanager.data.model.PdfExportFilterState()
    }

    fun dismissPdfExportMessage() {
        _pdfExportMessage.value = null
        _pdfExportError.value = null
    }

    fun generatePdfToUri(uri: android.net.Uri, context: Context) {
        if (_isGeneratingPdf.value) return
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            _pdfExportError.value = null
            try {
                val filterState = _pdfFilterState.value
                val backupData = repository.exportBackupData()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        com.engrshuvo.financemanager.data.export.PdfReportGenerator.generatePdf(
                            filterState = filterState,
                            transactions = backupData.transactions,
                            loans = backupData.loans,
                            repayments = backupData.loanRepayments,
                            allocations = backupData.budgetAllocations,
                            budgetSetting = backupData.budgetSettings.find { it.settingKey == com.engrshuvo.financemanager.data.model.BudgetSettingEntity.KEY_MONTHLY_BUDGET },
                            goals = backupData.financialGoals,
                            goalContributions = backupData.goalContributions,
                            outputStream = outputStream
                        )
                        outputStream.flush()
                    } ?: throw IllegalStateException("Unable to open output stream for selected file.")
                }
                _pdfExportFileUri.value = uri
                _pdfExportMessage.value = "${filterState.selectedReportType.title} exported successfully!"
            } catch (e: Exception) {
                _pdfExportError.value = "PDF Export failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun sharePdfReport(context: Context) {
        if (_isGeneratingPdf.value) return
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            _pdfExportError.value = null
            try {
                val filterState = _pdfFilterState.value
                val backupData = repository.exportBackupData()
                val fileUri = withContext(Dispatchers.IO) {
                    val exportDir = java.io.File(context.cacheDir, "pdf_exports").apply { if (!exists()) mkdirs() }
                    val tempFile = java.io.File(exportDir, filterState.getSuggestedFileName())
                    tempFile.outputStream().use { outputStream ->
                        com.engrshuvo.financemanager.data.export.PdfReportGenerator.generatePdf(
                            filterState = filterState,
                            transactions = backupData.transactions,
                            loans = backupData.loans,
                            repayments = backupData.loanRepayments,
                            allocations = backupData.budgetAllocations,
                            budgetSetting = backupData.budgetSettings.find { it.settingKey == com.engrshuvo.financemanager.data.model.BudgetSettingEntity.KEY_MONTHLY_BUDGET },
                            goals = backupData.financialGoals,
                            goalContributions = backupData.goalContributions,
                            outputStream = outputStream
                        )
                        outputStream.flush()
                    }
                    val authority = "${context.packageName}.fileprovider"
                    androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
                }
                _pdfExportFileUri.value = fileUri
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share ${filterState.selectedReportType.title}"))
            } catch (e: Exception) {
                _pdfExportError.value = "Failed to share PDF: ${e.message ?: "Unknown error"}"
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun openPdfReport(context: Context, uri: android.net.Uri) {
        try {
            val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            _pdfExportError.value = "No PDF viewer app found on device. Please install a PDF viewer."
        }
    }

    // ==========================================
    // PURCHASE PLANNER (SHOPPING NOTES)
    // Strictly isolated from financial accounting
    // ==========================================

    fun setPurchaseFilter(filter: PurchaseItemFilter) {
        _purchaseItemFilter.value = filter
    }

    fun setPurchaseSearchQuery(query: String) {
        _purchaseSearchQuery.value = query
    }

    fun openAddPurchaseItemDialog() {
        _editingPurchaseItem.value = null
        _isAddPurchaseDialogOpen.value = true
    }

    fun openEditPurchaseItemDialog(item: PurchaseItemEntity) {
        _editingPurchaseItem.value = item
        _isAddPurchaseDialogOpen.value = true
    }

    fun closePurchaseItemDialog() {
        _isAddPurchaseDialogOpen.value = false
        _editingPurchaseItem.value = null
    }

    fun savePurchaseItem(id: Long = 0L, title: String, note: String = "") {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        if (!isPurchaseItemSaving.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                if (id == 0L) {
                    val newItem = PurchaseItemEntity(
                        title = cleanTitle,
                        note = note.trim(),
                        isCompleted = false,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertPurchaseItem(newItem)
                } else {
                    val existing = _editingPurchaseItem.value ?: repository.allPurchaseItems.first().find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(
                            title = cleanTitle,
                            note = note.trim()
                        )
                        repository.updatePurchaseItem(updated)
                    }
                }
                closePurchaseItemDialog()
            } finally {
                isPurchaseItemSaving.set(false)
            }
        }
    }

    fun togglePurchaseItemComplete(item: PurchaseItemEntity) {
        viewModelScope.launch {
            if (item.isCompleted) {
                repository.restorePurchaseItemToPending(item.id)
            } else {
                repository.markPurchaseItemCompleted(item.id)
            }
        }
    }

    fun restorePurchaseItem(id: Long) {
        viewModelScope.launch {
            repository.restorePurchaseItemToPending(id)
        }
    }

    fun requestDeletePurchaseItem(item: PurchaseItemEntity) {
        _itemToDeletePurchase.value = item
    }

    fun dismissDeletePurchaseItemDialog() {
        _itemToDeletePurchase.value = null
    }

    fun confirmDeletePurchaseItem() {
        val toDelete = _itemToDeletePurchase.value ?: return
        viewModelScope.launch {
            repository.deletePurchaseItem(toDelete.id)
            _itemToDeletePurchase.value = null
        }
    }

    // ==========================================
    // PAYMENT SMS IMPORT ACTIONS
    // ==========================================

    fun updateSmsImportEnabled(enabled: Boolean, context: Context? = null) {
        viewModelScope.launch {
            paymentSmsPreferencesRepo?.updateEnabled(enabled)
            if (enabled) {
                _smsOperationMessage.value = "Payment SMS Import enabled. New incoming payment messages will be captured."
            } else {
                _smsOperationMessage.value = "Payment SMS Import disabled."
            }
        }
    }

    fun updateSmsAutoPost(autoPost: Boolean) {
        viewModelScope.launch {
            paymentSmsPreferencesRepo?.updateAutoPost(autoPost)
        }
    }

    fun openSmsRecordReview(record: com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity) {
        _smsRecordToReview.value = record
    }

    fun dismissSmsRecordReview() {
        _smsRecordToReview.value = null
    }

    fun approveSmsRecord(recordId: Long, categoryId: String, note: String = "") {
        viewModelScope.launch {
            val success = repository.approveSmsRecord(recordId, categoryId, note)
            if (success) {
                _smsOperationMessage.value = "Payment record approved and added to transactions!"
            }
            _smsRecordToReview.value = null
        }
    }

    fun dismissSmsRecord(recordId: Long) {
        viewModelScope.launch {
            repository.dismissSmsRecord(recordId)
            _smsRecordToReview.value = null
            _smsOperationMessage.value = "SMS candidate dismissed."
        }
    }

    fun deleteSmsRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteSmsRecord(recordId)
            _smsRecordToReview.value = null
        }
    }

    fun setSmsPasteInput(input: String) {
        _smsPasteInput.value = input
        _smsPasteTestResult.value = null
    }

    fun testParseSmsPaste(sender: String, body: String) {
        val parsed = com.engrshuvo.financemanager.sms.parser.PaymentSmsParser.parse(sender, body)
        if (parsed != null) {
            _smsPasteTestResult.value = "✅ Parsed Successfully!\nProvider: ${parsed.provider}\nType: ${parsed.type}\nAmount: ৳${String.format(Locale.US, "%.2f", parsed.amount)}\nFee: ৳${String.format(Locale.US, "%.2f", parsed.fee)}\nTrxID: ${parsed.trxId ?: "N/A"}\nCounterparty: ${parsed.counterparty.ifBlank { "N/A" }}\nSuggested Category: ${parsed.suggestedCategory}"
        } else {
            _smsPasteTestResult.value = "⚠️ Could not parse as a supported financial payment message or message is promotional/OTP."
        }
    }

    fun handleSharedText(text: String) {
        setSmsPasteInput(text)
        navigateToMoreSubDestination(MoreSubDestination.SMS_IMPORT)
    }

    fun importPastedSms(context: Context, sender: String, body: String) {
        viewModelScope.launch {
            val success = com.engrshuvo.financemanager.sms.PaymentSmsProcessor.processManualSms(
                context = context,
                sender = sender,
                body = body,
                messageTimestamp = System.currentTimeMillis()
            )
            if (success) {
                _smsOperationMessage.value = "SMS parsed and imported into records!"
                _smsPasteInput.value = ""
                _smsPasteTestResult.value = null
            } else {
                _smsOperationMessage.value = "Import rejected: duplicate TrxID or invalid payment format."
            }
        }
    }

    fun dismissSmsOperationMessage() {
        _smsOperationMessage.value = null
    }
}

class FinanceViewModelFactory(
    private val repository: FinanceRepository,
    private val notificationPreferencesRepo: NotificationPreferencesRepository? = null,
    private val paymentSmsPreferencesRepo: com.engrshuvo.financemanager.sms.PaymentSmsPreferencesRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(repository, notificationPreferencesRepo, paymentSmsPreferencesRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
