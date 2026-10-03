package com.engrshuvo.financemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
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
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class FinanceViewModel(
    private val repository: FinanceRepository
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

    private val _isAddTransactionSheetOpen = MutableStateFlow(false)
    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _defaultEntryType = MutableStateFlow(TransactionType.EXPENSE)
    private val _defaultLoanType = MutableStateFlow(LoanType.LENT)
    private val _isBudgetLimitDialogOpen = MutableStateFlow(false)
    private val _isBudgetPlanningDialogOpen = MutableStateFlow(false)
    private val _isDailyLimitDialogOpen = MutableStateFlow(false)

    // Archive & Recovery state flows
    private val _archiveFilterType = MutableStateFlow(ArchiveFilterType.ALL)
    private val _archiveSearchQuery = MutableStateFlow("")
    private val _itemToPermanentlyDelete = MutableStateFlow<ArchiveItemWrapper?>(null)
    private val _recentlyArchivedNote = MutableStateFlow<String?>(null)

    init {
        // Automatic cleanup on app startup
        viewModelScope.launch {
            repository.purgeExpiredArchivedRecords()
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

    private val archiveRawDataFlow: Flow<ArchiveData> = combine(
        repository.archivedTransactions,
        repository.archivedLoans
    ) { txs, loans ->
        ArchiveData(txs, loans)
    }.flowOn(Dispatchers.Default)

    private val archiveParamsFlow: Flow<ArchiveParams> = combine(
        _archiveFilterType,
        _archiveSearchQuery,
        _itemToPermanentlyDelete,
        _moreSubDestination
    ) { filterType, query, itemToDelete, moreDest ->
        ArchiveParams(filterType, query, itemToDelete, moreDest, _recentlyArchivedNote.value)
    }.combine(_recentlyArchivedNote) { params, note ->
        params.copy(recentlyArchivedNote = note)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<FinanceUiState> = combine(
        coreDataFlow,
        calendarParamsFlow,
        filterParamsFlow,
        archiveRawDataFlow,
        archiveParamsFlow
    ) { coreData, calendarParams, filterParams, archiveRawData, archiveParams ->
        val allTransactions = coreData.allTransactions
        val allLoans = coreData.allLoans
        val monthlyLimit = coreData.monthlyLimit
        val dailyLimit = coreData.dailyLimit
        val allAllocations = coreData.allAllocations
        val activeTab = coreData.activeTab

        // 1. Month boundaries for displayedMonth
        val cal = calendarParams.month.clone() as Calendar
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

        // Month-specific transactions (strictly bounded interval)
        val monthTransactions = allTransactions.filter { it.timestamp in startOfMonth..endOfMonth }

        // Monthly Income: ordinary income (excluding loan collections to keep operating income clean)
        val totalIncome = monthTransactions
            .filter { it.type == TransactionType.INCOME && it.categoryId != "loan_collected" }
            .sumOf { it.amount }

        // Monthly Expense: ordinary living expenses (excluding loan repayments to keep budget clean)
        val totalExpense = monthTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" }
            .sumOf { it.amount }

        val netOperatingCashChange = totalIncome - totalExpense

        // All-time available cash balance
        val allTimeIncome = allTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val allTimeExpense = allTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = allTimeIncome - allTimeExpense

        // 2. Today's expenses & daily budget limit
        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()
        val todayExpenses = allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" && it.timestamp in startOfToday..endOfToday }
            .sumOf { it.amount }

        val dailyBudgetRemaining = dailyLimit - todayExpenses
        val dailyBudgetProgress = if (dailyLimit > 0) (todayExpenses / dailyLimit).toFloat().coerceIn(0f, 1f) else 0f
        val isDailyOverBudget = todayExpenses > dailyLimit && dailyLimit > 0

        // 3. Month Allocations for selectedMonthKey
        val savedMonthAllocations = allAllocations.filter { it.monthKey == selectedMonthKey }

        // If current month has no saved allocations, pre-fill from previous month as editable draft
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMonthKey = String.format(Locale.US, "%04d-%02d", prevCal.get(Calendar.YEAR), prevCal.get(Calendar.MONTH) + 1)
        val prevMonthAllocations = if (savedMonthAllocations.isEmpty()) {
            allAllocations.filter { it.monthKey == prevMonthKey }
        } else {
            emptyList()
        }

        val allocationsSourceMap = if (savedMonthAllocations.isNotEmpty()) {
            savedMonthAllocations.associateBy { it.categoryId }
        } else {
            prevMonthAllocations.associateBy { it.categoryId }
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

        // Calculate suggested daily limit from Daily Expenses allocation without mutating state
        val dailyExpenseAlloc = monthCategoryAllocations.find { it.category.id == "daily_expenses" }?.allocatedAmount ?: 0.0
        val suggestedDailyLimit = if (dailyExpenseAlloc > 0 && daysInMonth > 0) dailyExpenseAlloc / daysInMonth else 0.0

        // 4. Loans summary
        val activeLoans = allLoans.filter { it.status == LoanStatus.ACTIVE }
        val totalActiveLent = activeLoans.filter { it.type == LoanType.LENT }.sumOf { it.remainingAmount }
        val totalActiveBorrowed = activeLoans.filter { it.type == LoanType.BORROWED }.sumOf { it.remainingAmount }

        // 5. Category Breakdown for selected month (ordinary expenses only)
        val monthOrdinaryExpenses = monthTransactions.filter { it.type == TransactionType.EXPENSE && it.categoryId != "loan_repaid" }
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

        // 6. Calendar Days
        val calendarDays = generateCalendarGrid(
            calendarMonth = calendarParams.month,
            selectedDateTs = calendarParams.selectedDate,
            transactions = allTransactions
        )

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

        val now = System.currentTimeMillis()
        val txWrappers = archiveRawData.archivedTransactions.map { entity ->
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

        val loanWrappers = archiveRawData.archivedLoans.map { entity ->
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

        val allArchivedItems = (txWrappers + loanWrappers).sortedByDescending { it.archivedAt }
        val filteredArchivedItems = allArchivedItems.filter { item ->
            val matchesType = when (archiveParams.filterType) {
                ArchiveFilterType.ALL -> true
                ArchiveFilterType.INCOME -> item is ArchiveItemWrapper.Transaction && item.entity.type == TransactionType.INCOME
                ArchiveFilterType.EXPENSE -> item is ArchiveItemWrapper.Transaction && item.entity.type == TransactionType.EXPENSE
                ArchiveFilterType.LOANS -> item is ArchiveItemWrapper.Loan
            }
            val matchesQuery = if (archiveParams.searchQuery.isBlank()) true else {
                val q = archiveParams.searchQuery.trim().lowercase()
                item.title.lowercase().contains(q) || item.amount.toString().contains(q)
            }
            matchesType && matchesQuery
        }

        FinanceUiState(
            activeTab = activeTab,
            moreSubDestination = archiveParams.moreDest,
            displayedMonth = calendarParams.month,
            selectedDateTimestamp = calendarParams.selectedDate,
            calendarDays = calendarDays,
            selectedDayTransactions = selectedDayTransactions,
            selectedDaySummary = selectedDaySummary,
            isDayDetailSheetOpen = calendarParams.isSheetOpen,
            selectedMonthKey = selectedMonthKey,
            balance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netOperatingCashChange = netOperatingCashChange,
            totalActiveLent = totalActiveLent,
            totalActiveBorrowed = totalActiveBorrowed,
            monthAllocations = monthCategoryAllocations,
            totalAllocated = totalAllocated,
            unallocatedIncome = unallocatedIncome,
            plannedShortfall = plannedShortfall,
            isShortfall = isShortfall,
            plannedSavingsTotal = plannedSavingsTotal,
            todayExpenses = todayExpenses,
            dailyBudgetLimit = dailyLimit,
            dailyBudgetRemaining = dailyBudgetRemaining,
            dailyBudgetProgress = dailyBudgetProgress,
            isDailyOverBudget = isDailyOverBudget,
            suggestedDailyLimit = suggestedDailyLimit,
            monthlyLimit = monthlyLimit,
            monthlySpent = monthlySpent,
            budgetProgress = budgetProgress,
            budgetRemaining = budgetRemaining,
            allTransactions = allTransactions,
            filteredTransactions = filteredTransactions,
            categorySpendBreakdown = categoryBreakdown,
            searchQuery = search,
            selectedTypeFilter = typeFilter,
            selectedCategoryFilterId = categoryFilter,
            selectedDateFilter = dateFilter,
            allLoans = allLoans,
            filteredLoans = filteredLoans,
            selectedLoanFilter = loanTypeFilter,
            selectedLoanStatusFilter = loanStatusFilter,
            repayingLoan = _repayingLoan.value,
            isRepayDialogOpen = _isRepayDialogOpen.value,
            archivedItems = allArchivedItems,
            filteredArchivedItems = filteredArchivedItems,
            archiveFilterType = archiveParams.filterType,
            archiveSearchQuery = archiveParams.searchQuery,
            itemToPermanentlyDelete = archiveParams.itemToDelete,
            recentlyArchivedNote = archiveParams.recentlyArchivedNote,
            isAddTransactionSheetOpen = _isAddTransactionSheetOpen.value,
            editingTransaction = _editingTransaction.value,
            defaultEntryType = _defaultEntryType.value,
            defaultLoanType = _defaultLoanType.value,
            isBudgetLimitDialogOpen = _isBudgetLimitDialogOpen.value,
            isBudgetPlanningDialogOpen = _isBudgetPlanningDialogOpen.value,
            isDailyLimitDialogOpen = _isDailyLimitDialogOpen.value,
            currencySymbol = "৳"
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
        _editingTransaction.value = null
        _defaultEntryType.value = type
        _defaultLoanType.value = loanType
        _isAddTransactionSheetOpen.value = true
    }

    fun openEditTransactionSheet(transaction: TransactionEntity) {
        _editingTransaction.value = transaction
        _defaultEntryType.value = transaction.type
        _isAddTransactionSheetOpen.value = true
    }

    fun closeAddTransactionSheet() {
        _isAddTransactionSheetOpen.value = false
        _editingTransaction.value = null
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        _isRepayDialogOpen.value = true
    }

    fun closeRepayDialog() {
        _isRepayDialogOpen.value = false
        _repayingLoan.value = null
    }

    fun submitLoanRepayment(loanId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordLoanRepayment(loanId, amount, note)
            closeRepayDialog()
        }
    }

    fun markLoanSettled(loanId: Long) {
        viewModelScope.launch {
            repository.markLoanSettled(loanId)
        }
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
        if (tab != FinanceTab.MORE) {
            _moreSubDestination.value = MoreSubDestination.NONE
        }
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
            }
            closePermanentDeleteDialog()
        }
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
}

class FinanceViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
