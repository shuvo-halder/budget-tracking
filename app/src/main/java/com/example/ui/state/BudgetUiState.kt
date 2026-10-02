package com.example.ui.state

import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType

enum class TransactionTypeFilter {
    ALL,
    INCOME,
    EXPENSE
}

enum class DateFilterOption(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    TODAY("Today")
}

data class CategorySpending(
    val category: TransactionCategory,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class BudgetUiState(
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
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
    val isAddEditSheetOpen: Boolean = false,
    val editingTransaction: TransactionEntity? = null,
    val defaultSheetType: TransactionType = TransactionType.EXPENSE,
    val isBudgetLimitDialogOpen: Boolean = false,
    val currencySymbol: String = "৳"
)
