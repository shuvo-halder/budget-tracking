package com.example.ui.model

data class CalendarDayCell(
    val timestamp: Long,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasIncome: Boolean = false,
    val hasExpense: Boolean = false,
    val hasLoan: Boolean = false,
    val dayIncomeTotal: Double = 0.0,
    val dayExpenseTotal: Double = 0.0,
    val dayLoanTotal: Double = 0.0,
    val transactionCount: Int = 0
)

data class DaySummaryStats(
    val dateTimestamp: Long,
    val formattedDate: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalLoan: Double,
    val netBalance: Double
)
