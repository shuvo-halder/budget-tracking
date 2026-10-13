package com.engrshuvo.financemanager.data.model

import androidx.compose.runtime.Immutable
import com.engrshuvo.financemanager.ui.state.LoanStatusFilter
import com.engrshuvo.financemanager.ui.state.LoanTypeFilter
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportCategory(val displayName: String) {
    INCOME_AND_EXPENSES("Income & Expenses"),
    BUDGET_AND_SAVINGS("Budget & Savings"),
    LOANS("Loans"),
    CUSTOM("Custom Period")
}

enum class ReportType(
    val id: String,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val defaultLandscape: Boolean = false
) {
    MONTHLY_EXPENSE(
        id = "monthly_expense",
        title = "Monthly Expense Report",
        description = "Detailed breakdown of all actual expenses for a selected calendar month.",
        category = ReportCategory.INCOME_AND_EXPENSES
    ),
    MONTHLY_INCOME(
        id = "monthly_income",
        title = "Monthly Income Report",
        description = "Summary of salary, earnings, and ordinary income receipts for a selected month.",
        category = ReportCategory.INCOME_AND_EXPENSES
    ),
    DAILY_EXPENSE(
        id = "daily_expense",
        title = "Daily Expense Report",
        description = "Itemized log of expenses incurred on a specific single day.",
        category = ReportCategory.INCOME_AND_EXPENSES
    ),
    CATEGORY_EXPENSE(
        id = "category_expense",
        title = "Category-wise Expense Report",
        description = "Spending distribution and category percentages for a selected month.",
        category = ReportCategory.INCOME_AND_EXPENSES
    ),
    TRANSACTION_HISTORY(
        id = "transaction_history",
        title = "Transaction History",
        description = "Comprehensive ledger of income, expense, and loan records over a date range.",
        category = ReportCategory.INCOME_AND_EXPENSES,
        defaultLandscape = true
    ),
    MONTHLY_SUMMARY(
        id = "monthly_summary",
        title = "Monthly Financial Summary",
        description = "Executive financial overview showing cash flow, net savings, and budget status.",
        category = ReportCategory.INCOME_AND_EXPENSES
    ),
    BUDGET_VS_ACTUAL(
        id = "budget_vs_actual",
        title = "Budget vs Actual Report",
        description = "Category-by-category comparison between planned budget targets and actual spending.",
        category = ReportCategory.BUDGET_AND_SAVINGS
    ),
    SAVINGS_REPORT(
        id = "savings_report",
        title = "Savings Report",
        description = "Analysis of planned savings reserves and goal contribution deposits.",
        category = ReportCategory.BUDGET_AND_SAVINGS
    ),
    FINANCIAL_GOALS(
        id = "financial_goals",
        title = "Financial Goals Report",
        description = "Status, target dates, remaining amounts, and deposit progress for savings goals.",
        category = ReportCategory.BUDGET_AND_SAVINGS
    ),
    LOAN_SUMMARY(
        id = "loan_summary",
        title = "Loan Summary Report",
        description = "Active and settled loan principal, outstanding balances for Lent and Borrowed debts.",
        category = ReportCategory.LOANS
    ),
    LOAN_REPAYMENT_HISTORY(
        id = "loan_repayment_history",
        title = "Loan Repayment History",
        description = "Chronological record of repayments and collections for active and settled loans.",
        category = ReportCategory.LOANS
    ),
    CUSTOM_DATE_TRANSACTIONS(
        id = "custom_date_transactions",
        title = "Custom Date Range Report",
        description = "Filtered ledger of financial records strictly within a custom start and end date.",
        category = ReportCategory.CUSTOM,
        defaultLandscape = true
    );

    companion object {
        fun getById(id: String): ReportType =
            values().firstOrNull { it.id == id } ?: MONTHLY_EXPENSE
    }
}

@Immutable
data class PdfExportFilterState(
    val selectedReportType: ReportType = ReportType.MONTHLY_EXPENSE,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1, // 1..12
    val selectedDailyTimestamp: Long = System.currentTimeMillis(),
    val startDateTimestamp: Long = getStartOfMonthTimestamp(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH) + 1),
    val endDateTimestamp: Long = System.currentTimeMillis(),
    val selectedCategoryFilterId: String? = null, // null means "All Categories"
    val selectedTransactionTypeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val selectedLoanTypeFilter: LoanTypeFilter = LoanTypeFilter.ALL,
    val selectedGoalStatusFilter: GoalStatus? = null // null means "All Goals"
) {
    fun getFormattedMonthYear(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    fun getMonthKey(): String {
        return String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth)
    }

    fun getSuggestedFileName(): String {
        val typePart = selectedReportType.id.replace("_", "-").replaceFirstChar { it.uppercase() }
        val datePart = when (selectedReportType) {
            ReportType.MONTHLY_EXPENSE,
            ReportType.MONTHLY_INCOME,
            ReportType.CATEGORY_EXPENSE,
            ReportType.MONTHLY_SUMMARY,
            ReportType.BUDGET_VS_ACTUAL,
            ReportType.SAVINGS_REPORT -> getMonthKey()

            ReportType.DAILY_EXPENSE -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(selectedDailyTimestamp))

            ReportType.TRANSACTION_HISTORY,
            ReportType.LOAN_REPAYMENT_HISTORY,
            ReportType.CUSTOM_DATE_TRANSACTIONS -> {
                val start = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startDateTimestamp))
                val end = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(endDateTimestamp))
                "${start}_to_${end}"
            }

            ReportType.LOAN_SUMMARY,
            ReportType.FINANCIAL_GOALS -> getMonthKey()
        }
        return "${typePart}_${datePart}.pdf"
    }

    companion object {
        fun getStartOfMonthTimestamp(year: Int, month: Int): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun getEndOfMonthTimestamp(year: Int, month: Int): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }
    }
}

@Immutable
data class PdfExportSummary(
    val reportTitle: String,
    val periodDescription: String,
    val filterDescription: String,
    val recordCount: Int,
    val totalAmountText: String? = null,
    val isValidPeriod: Boolean = true,
    val validationErrorMessage: String? = null
)
