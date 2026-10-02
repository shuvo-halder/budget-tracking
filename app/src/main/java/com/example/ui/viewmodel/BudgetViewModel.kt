package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryCatalog
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.BudgetRepository
import com.example.ui.state.BudgetUiState
import com.example.ui.state.CategorySpending
import com.example.ui.state.DateFilterOption
import com.example.ui.state.TransactionTypeFilter
import com.example.ui.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    private val _selectedCategoryFilterId = MutableStateFlow<String?>(null)
    private val _selectedDateFilter = MutableStateFlow(DateFilterOption.ALL_TIME)

    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _defaultSheetType = MutableStateFlow(TransactionType.EXPENSE)
    private val _isBudgetLimitDialogOpen = MutableStateFlow(false)

    private data class TotalsData(
        val totalIncome: Double,
        val totalExpense: Double,
        val monthlyLimit: Double
    )

    private data class FilterParams(
        val search: String,
        val typeFilter: TransactionTypeFilter,
        val categoryFilterId: String?,
        val dateFilter: DateFilterOption
    )

    private data class DialogStates(
        val isAddEditOpen: Boolean,
        val editingTransaction: TransactionEntity?,
        val defaultSheetType: TransactionType,
        val isBudgetDialogOpen: Boolean
    )

    private val totalsFlow: Flow<TotalsData> = combine(
        repository.totalIncome,
        repository.totalExpense,
        repository.monthlyBudgetLimit
    ) { income, expense, limit ->
        TotalsData(income, expense, limit)
    }

    private val filtersFlow: Flow<FilterParams> = combine(
        _searchQuery,
        _selectedTypeFilter,
        _selectedCategoryFilterId,
        _selectedDateFilter
    ) { search, type, category, date ->
        FilterParams(search, type, category, date)
    }

    private val dialogsFlow: Flow<DialogStates> = combine(
        _isAddEditSheetOpen,
        _editingTransaction,
        _defaultSheetType,
        _isBudgetLimitDialogOpen
    ) { isOpen, editing, defaultType, isBudgetOpen ->
        DialogStates(isOpen, editing, defaultType, isBudgetOpen)
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.allTransactions,
        totalsFlow,
        filtersFlow,
        dialogsFlow
    ) { allTransactions: List<TransactionEntity>, totals: TotalsData, filters: FilterParams, dialogs: DialogStates ->
        val totalIncome = totals.totalIncome
        val totalExpense = totals.totalExpense
        val monthlyLimit = totals.monthlyLimit
        val balance = totalIncome - totalExpense

        // Calculate monthly expense for budget tracking
        val startOfMonth = DateUtils.getStartOfMonth()
        val monthlySpent = allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= startOfMonth }
            .sumOf { it.amount }

        val budgetProgress = if (monthlyLimit > 0) {
            (monthlySpent / monthlyLimit).toFloat()
        } else 0f

        val budgetRemaining = (monthlyLimit - monthlySpent).coerceAtLeast(0.0)

        // Calculate category spending breakdown (Expenses only)
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
        } else {
            emptyList()
        }

        // Apply filters to transactions list
        val filtered = allTransactions.filter { item ->
            // Filter by Type
            val matchesType = when (filters.typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.INCOME -> item.type == TransactionType.INCOME
                TransactionTypeFilter.EXPENSE -> item.type == TransactionType.EXPENSE
            }

            // Filter by Category
            val matchesCategory = filters.categoryFilterId == null || item.categoryId == filters.categoryFilterId

            // Filter by Date
            val matchesDate = when (filters.dateFilter) {
                DateFilterOption.ALL_TIME -> true
                DateFilterOption.THIS_MONTH -> item.timestamp >= DateUtils.getStartOfMonth()
                DateFilterOption.THIS_WEEK -> item.timestamp >= DateUtils.getStartOfWeek()
                DateFilterOption.TODAY -> item.timestamp >= DateUtils.getStartOfDay()
            }

            // Filter by Search Query
            val matchesSearch = if (filters.search.isBlank()) {
                true
            } else {
                val query = filters.search.trim().lowercase()
                item.note.lowercase().contains(query) ||
                        item.categoryName.lowercase().contains(query) ||
                        item.amount.toString().contains(query)
            }

            matchesType && matchesCategory && matchesDate && matchesSearch
        }

        BudgetUiState(
            balance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthlyLimit = monthlyLimit,
            monthlySpent = monthlySpent,
            budgetProgress = budgetProgress,
            budgetRemaining = budgetRemaining,
            allTransactions = allTransactions,
            filteredTransactions = filtered,
            categorySpendBreakdown = categoryBreakdown,
            searchQuery = filters.search,
            selectedTypeFilter = filters.typeFilter,
            selectedCategoryFilterId = filters.categoryFilterId,
            selectedDateFilter = filters.dateFilter,
            isAddEditSheetOpen = dialogs.isAddEditOpen,
            editingTransaction = dialogs.editingTransaction,
            defaultSheetType = dialogs.defaultSheetType,
            isBudgetLimitDialogOpen = dialogs.isBudgetDialogOpen,
            currencySymbol = "৳"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState()
    )

    fun openAddSheet(type: TransactionType = TransactionType.EXPENSE) {
        _editingTransaction.value = null
        _defaultSheetType.value = type
        _isAddEditSheetOpen.value = true
    }

    fun openEditSheet(transaction: TransactionEntity) {
        _editingTransaction.value = transaction
        _defaultSheetType.value = transaction.type
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditSheetOpen.value = false
        _editingTransaction.value = null
    }

    fun openBudgetLimitDialog() {
        _isBudgetLimitDialogOpen.value = true
    }

    fun closeBudgetLimitDialog() {
        _isBudgetLimitDialogOpen.value = false
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
            closeAddEditSheet()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun updateMonthlyBudgetLimit(limit: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudgetLimit(limit)
            closeBudgetLimitDialog()
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

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedTypeFilter.value = TransactionTypeFilter.ALL
        _selectedCategoryFilterId.value = null
        _selectedDateFilter.value = DateFilterOption.ALL_TIME
    }
}
