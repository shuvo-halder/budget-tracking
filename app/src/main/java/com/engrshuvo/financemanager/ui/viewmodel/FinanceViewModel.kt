package com.engrshuvo.financemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.ui.model.CalendarDayCell
import com.engrshuvo.financemanager.ui.model.DaySummaryStats
import com.engrshuvo.financemanager.ui.state.CategorySpending
import com.engrshuvo.financemanager.ui.state.DateFilterOption
import com.engrshuvo.financemanager.ui.state.FinanceTab
import com.engrshuvo.financemanager.ui.state.FinanceUiState
import com.engrshuvo.financemanager.ui.state.LoanStatusFilter
import com.engrshuvo.financemanager.ui.state.LoanTypeFilter
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

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(FinanceTab.CALENDAR)
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
        val monthlyLimit: Double
    )

    private val coreDataFlow: Flow<CoreData> = combine(
        _activeTab,
        repository.allTransactions,
        repository.allLoans,
        repository.monthlyBudgetLimit
    ) { activeTab, transactions, loans, budgetLimit ->
        CoreData(activeTab, transactions, loans, budgetLimit)
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

    val uiState: StateFlow<FinanceUiState> = combine(
        coreDataFlow,
        calendarParamsFlow,
        filterParamsFlow
    ) { coreData, calendarParams, filterParams ->
        val allTransactions = coreData.allTransactions
        val allLoans = coreData.allLoans
        val monthlyLimit = coreData.monthlyLimit
        val activeTab = coreData.activeTab

        val totalIncome = allTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = allTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val activeLoans = allLoans.filter { it.status == LoanStatus.ACTIVE }
        val totalActiveLent = activeLoans.filter { it.type == LoanType.LENT }.sumOf { it.remainingAmount }
        val totalActiveBorrowed = activeLoans.filter { it.type == LoanType.BORROWED }.sumOf { it.remainingAmount }

        val startOfMonth = DateUtils.getStartOfMonth()
        val monthlySpent = allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= startOfMonth }
            .sumOf { it.amount }

        val budgetProgress = if (monthlyLimit > 0) (monthlySpent / monthlyLimit).toFloat() else 0f
        val budgetRemaining = (monthlyLimit - monthlySpent).coerceAtLeast(0.0)

        val expenseTransactions = allTransactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenseForBreakdown = expenseTransactions.sumOf { it.amount }
        val categoryBreakdown = if (totalExpenseForBreakdown > 0) {
            expenseTransactions
                .groupBy { it.categoryId }
                .map { (catId, items) ->
                    val catAmount = items.sumOf { it.amount }
                    val category = CategoryCatalog.getCategoryById(catId)
                    CategorySpending(
                        category = category,
                        totalAmount = catAmount,
                        percentage = (catAmount / totalExpenseForBreakdown).toFloat(),
                        transactionCount = items.size
                    )
                }
                .sortedByDescending { it.totalAmount }
        } else emptyList()

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
                DateFilterOption.THIS_MONTH -> item.timestamp >= DateUtils.getStartOfMonth()
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

        FinanceUiState(
            activeTab = activeTab,
            displayedMonth = calendarParams.month,
            selectedDateTimestamp = calendarParams.selectedDate,
            calendarDays = calendarDays,
            selectedDayTransactions = selectedDayTransactions,
            selectedDaySummary = selectedDaySummary,
            isDayDetailSheetOpen = calendarParams.isSheetOpen,
            balance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalActiveLent = totalActiveLent,
            totalActiveBorrowed = totalActiveBorrowed,
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
            isAddTransactionSheetOpen = _isAddTransactionSheetOpen.value,
            editingTransaction = _editingTransaction.value,
            defaultEntryType = _defaultEntryType.value,
            defaultLoanType = _defaultLoanType.value,
            isBudgetLimitDialogOpen = _isBudgetLimitDialogOpen.value,
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
