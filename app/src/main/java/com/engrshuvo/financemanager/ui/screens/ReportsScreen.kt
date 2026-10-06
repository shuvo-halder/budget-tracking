package com.engrshuvo.financemanager.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.ui.model.CalendarDayCell
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import com.engrshuvo.financemanager.ui.state.CategorySpending
import com.engrshuvo.financemanager.ui.state.FinanceUiState
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.theme.LoanBlue
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = remember(uiState.displayedMonth) {
        DateUtils.formatMonthYear(uiState.displayedMonth)
    }

    val isCurrentMonthSelected = remember(uiState.displayedMonth) {
        val now = Calendar.getInstance()
        uiState.displayedMonth.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                uiState.displayedMonth.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }

    val currentMonthDays = remember(uiState.calendarDays) {
        uiState.calendarDays.filter { it.isCurrentMonth }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen_container")
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Reports & Analysis",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Comprehensive financial intelligence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("reports_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to More"
                    )
                }
            },
            actions = {
                if (!isCurrentMonthSelected) {
                    IconButton(
                        onClick = onResetToToday,
                        modifier = Modifier.testTag("reports_reset_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Reset to current month",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("reports_scrollable_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector Header
            item(key = "report_period_selector") {
                ReportPeriodSelectorCard(
                    monthName = monthName,
                    isCurrentMonth = isCurrentMonthSelected,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onResetToToday = onResetToToday
                )
            }

            // 2. Financial Overview Cards (4-metric grid)
            item(key = "report_financial_overview") {
                FinancialOverviewSection(
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    netOperatingCashChange = uiState.netOperatingCashChange,
                    totalBalance = uiState.balance
                )
            }

            // 3. Dynamic Automated Smart Insights
            item(key = "report_smart_insights") {
                SmartFinancialInsightsCard(
                    uiState = uiState,
                    currentMonthDays = currentMonthDays
                )
            }

            // 4. Income vs Expense Trend Chart
            item(key = "report_income_expense_trend") {
                IncomeExpenseTrendChartCard(
                    monthName = monthName,
                    currentMonthDays = currentMonthDays,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense
                )
            }

            // 5. Expense Categories Breakdown (Donut + Ranking)
            item(key = "report_category_breakdown") {
                ExpenseCategoryBreakdownCard(
                    breakdowns = uiState.categorySpendBreakdown,
                    totalExpense = uiState.totalExpense
                )
            }

            // 6. Budget vs Actual Execution
            item(key = "report_budget_vs_actual") {
                BudgetVsActualReportCard(
                    monthAllocations = uiState.monthAllocations,
                    totalAllocated = uiState.totalAllocated,
                    unallocatedIncome = uiState.unallocatedIncome,
                    plannedShortfall = uiState.plannedShortfall,
                    isShortfall = uiState.isShortfall
                )
            }

            // 7. Daily Spending Behavior & Limit Adherence
            item(key = "report_daily_spending") {
                DailySpendingAnalysisCard(
                    currentMonthDays = currentMonthDays,
                    dailyLimit = uiState.dailyBudgetLimit,
                    totalExpense = uiState.totalExpense
                )
            }

            // 8. Savings & Wealth Accumulation Analysis
            item(key = "report_savings_analysis") {
                SavingsAnalysisReportCard(
                    plannedSavings = uiState.plannedSavingsTotal,
                    netOperatingCashChange = uiState.netOperatingCashChange,
                    totalIncome = uiState.totalIncome
                )
            }

            // 9. Debt & Loan Portfolio Analysis
            item(key = "report_loan_analysis") {
                LoanPortfolioAnalysisCard(
                    allLoans = uiState.allLoans,
                    activeLent = uiState.totalActiveLent,
                    activeBorrowed = uiState.totalActiveBorrowed
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. Period Selector Card
// -------------------------------------------------------------------------------------------------

@Composable
private fun ReportPeriodSelectorCard(
    monthName: String,
    isCurrentMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_period_selector_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("report_prev_month")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous month"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { if (!isCurrentMonth) onResetToToday() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isCurrentMonth) "Current Month Period" else "Tap to return to today",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("report_next_month")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next month"
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. Financial Overview 4-Pill Section
// -------------------------------------------------------------------------------------------------

@Composable
private fun FinancialOverviewSection(
    totalIncome: Double,
    totalExpense: Double,
    netOperatingCashChange: Double,
    totalBalance: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReportMetricCard(
                title = "Total Income",
                amount = totalIncome,
                subtitle = "Month ordinary earnings",
                color = IncomeGreen,
                icon = Icons.Default.ArrowDownward,
                modifier = Modifier.weight(1f),
                testTag = "metric_total_income"
            )

            ReportMetricCard(
                title = "Total Expense",
                amount = totalExpense,
                subtitle = "Month ordinary spending",
                color = ExpenseRed,
                icon = Icons.Default.ArrowUpward,
                modifier = Modifier.weight(1f),
                testTag = "metric_total_expense"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isNetPositive = netOperatingCashChange >= 0
            val netColor = if (isNetPositive) IncomeGreen else ExpenseRed

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 96.dp)
                    .testTag("metric_net_cash_flow"),
                shape = RoundedCornerShape(16.dp),
                color = netColor.copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Cash Flow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = netColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isNetPositive) "+ Surplus" else "- Deficit",
                                color = netColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = CurrencyUtils.formatBDTWithSign(netOperatingCashChange, isNetPositive),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = netColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Income minus expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 96.dp)
                    .testTag("metric_total_balance"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = CurrencyUtils.formatBDT(totalBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "All-time ledger balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    amount: Double,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        modifier = modifier
            .heightIn(min = 96.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = CurrencyUtils.formatBDT(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. Smart Automated Insights Card
// -------------------------------------------------------------------------------------------------

@Composable
private fun SmartFinancialInsightsCard(
    uiState: FinanceUiState,
    currentMonthDays: List<CalendarDayCell>
) {
    val brandPrimary = MaterialTheme.colorScheme.primary
    val insights = remember(uiState, currentMonthDays, brandPrimary) {
        val list = mutableListOf<Triple<String, String, Color>>()

        // 1. Top Category Insight
        if (uiState.categorySpendBreakdown.isNotEmpty()) {
            val top = uiState.categorySpendBreakdown.first()
            val percent = (top.percentage * 100).toInt()
            list.add(
                Triple(
                    "Top Spending Driver",
                    "${top.category.name} is your highest expense (${CurrencyUtils.formatBDT(top.totalAmount)}, $percent% of total spend).",
                    top.category.color
                )
            )
        }

        // 2. Daily limit adherence
        val exceededDays = currentMonthDays.count { it.dayExpenseTotal > uiState.dailyBudgetLimit }
        val activeDays = currentMonthDays.count { it.dayExpenseTotal > 0 }
        if (activeDays > 0) {
            if (exceededDays == 0) {
                list.add(
                    Triple(
                        "Daily Limit Discipline",
                        "Great discipline! All spending across $activeDays active days stayed within your ${CurrencyUtils.formatBDT(uiState.dailyBudgetLimit)} daily limit.",
                        IncomeGreen
                    )
                )
            } else {
                list.add(
                    Triple(
                        "Daily Limit Attention",
                        "Daily expenses exceeded your ${CurrencyUtils.formatBDT(uiState.dailyBudgetLimit)} limit on $exceededDays of $activeDays spending days.",
                        ExpenseRed
                    )
                )
            }
        }

        // 3. Cash Flow Health
        if (uiState.totalIncome > 0 || uiState.totalExpense > 0) {
            if (uiState.netOperatingCashChange >= 0) {
                val savingsRate = if (uiState.totalIncome > 0) ((uiState.netOperatingCashChange / uiState.totalIncome) * 100).toInt() else 0
                list.add(
                    Triple(
                        "Cash Flow Health",
                        "Positive cash flow (+${CurrencyUtils.formatBDT(uiState.netOperatingCashChange)}) with an operating surplus rate of $savingsRate%.",
                        IncomeGreen
                    )
                )
            } else {
                list.add(
                    Triple(
                        "Operating Deficit",
                        "Expenses exceed received income by ${CurrencyUtils.formatBDT(abs(uiState.netOperatingCashChange))} for this monthly period.",
                        ExpenseRed
                    )
                )
            }
        }

        // 4. Budget Planning Status
        if (uiState.monthAllocations.any { it.allocatedAmount > 0 }) {
            val overBudgetCats = uiState.monthAllocations.count { it.isOverBudget }
            if (overBudgetCats > 0) {
                list.add(
                    Triple(
                        "Envelope Warning",
                        "$overBudgetCats budget envelope categories have exceeded their planned targets this month.",
                        ExpenseRed
                    )
                )
            } else {
                val totalRemaining = uiState.monthAllocations.sumOf { it.remainingAmount.coerceAtLeast(0.0) }
                list.add(
                    Triple(
                        "Budget Coverage",
                        "${CurrencyUtils.formatBDT(totalRemaining)} remaining across your planned category envelopes.",
                        brandPrimary
                    )
                )
            }
        }

        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_smart_insights_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Automated Financial Insights",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Data-driven summary of current execution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (insights.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Record transactions and budget allocations to generate real-time automated insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                insights.forEach { (title, body, color) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .padding(top = 4.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. Income vs Expense Trend Chart (Canvas Line/Area)
// -------------------------------------------------------------------------------------------------

@Composable
private fun IncomeExpenseTrendChartCard(
    monthName: String,
    currentMonthDays: List<CalendarDayCell>,
    totalIncome: Double,
    totalExpense: Double
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "trendChartAnim"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val hasData = remember(totalIncome, totalExpense) {
        totalIncome > 0 || totalExpense > 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_trend_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Income vs Expense Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily cash flow across $monthName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendItem(label = "Income", color = IncomeGreen)
                    ChartLegendItem(label = "Expense", color = ExpenseRed)
                }
            }

            if (!hasData || currentMonthDays.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No income or expense recorded for $monthName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val maxDailyAmount = remember(currentMonthDays) {
                    max(100.0, currentMonthDays.maxOfOrNull { max(it.dayIncomeTotal, it.dayExpenseTotal) } ?: 100.0)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = height - bottomPadding
                    val daysCount = currentMonthDays.size

                    if (daysCount < 2) return@Canvas

                    val stepX = width / (daysCount - 1).coerceAtLeast(1)

                    // Draw 3 horizontal guideline dashes
                    val gridColor = Color.LightGray.copy(alpha = 0.4f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    for (i in 1..3) {
                        val y = chartHeight * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // Build Income and Expense Path
                    val incomePath = Path()
                    val expensePath = Path()

                    currentMonthDays.forEachIndexed { index, day ->
                        val x = index * stepX
                        val incomeNormalized = ((day.dayIncomeTotal / maxDailyAmount).toFloat() * progress).coerceIn(0f, 1f)
                        val expenseNormalized = ((day.dayExpenseTotal / maxDailyAmount).toFloat() * progress).coerceIn(0f, 1f)

                        val yIncome = chartHeight - (incomeNormalized * chartHeight)
                        val yExpense = chartHeight - (expenseNormalized * chartHeight)

                        if (index == 0) {
                            incomePath.moveTo(x, yIncome)
                            expensePath.moveTo(x, yExpense)
                        } else {
                            incomePath.lineTo(x, yIncome)
                            expensePath.lineTo(x, yExpense)
                        }

                        // Draw small peak dots for active days
                        if (day.dayIncomeTotal > 0) {
                            drawCircle(
                                color = IncomeGreen,
                                radius = 3.5.dp.toPx(),
                                center = Offset(x, yIncome)
                            )
                        }
                        if (day.dayExpenseTotal > 0) {
                            drawCircle(
                                color = ExpenseRed,
                                radius = 3.5.dp.toPx(),
                                center = Offset(x, yExpense)
                            )
                        }
                    }

                    // Stroke Lines
                    drawPath(
                        path = incomePath,
                        color = IncomeGreen,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = expensePath,
                        color = ExpenseRed,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // X-Axis day markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Day 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("Day 10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("Day 20", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("Day ${currentMonthDays.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 5. Expense Category Breakdown (Donut & Ranked List)
// -------------------------------------------------------------------------------------------------

@Composable
private fun ExpenseCategoryBreakdownCard(
    breakdowns: List<CategorySpending>,
    totalExpense: Double
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donutReportAnim"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_category_breakdown_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Expense by Category",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Actual spending distribution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${breakdowns.size} Categories",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (breakdowns.isEmpty() || totalExpense <= 0.0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No ordinary expenses recorded in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 22.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                (size.width - diameter) / 2,
                                (size.height - diameter) / 2
                            )
                            val arcSize = Size(diameter, diameter)

                            var currentAngle = -90f
                            breakdowns.forEach { item ->
                                val sweep = (item.percentage * 360f) * animateProgress
                                if (sweep > 0.5f) {
                                    drawArc(
                                        color = item.category.color,
                                        startAngle = currentAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    currentAngle += sweep
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = CurrencyUtils.formatBDT(totalExpense),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Top 4 preview legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        breakdowns.take(4).forEach { item ->
                            val percent = (item.percentage * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(item.category.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.category.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$percent%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Full Ranked List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    breakdowns.forEach { item ->
                        val percent = (item.percentage * 100).toInt()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(item.category.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.category.icon,
                                            contentDescription = null,
                                            tint = item.category.color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.transactionCount} entries • $percent% of expenses",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Text(
                                    text = CurrencyUtils.formatBDT(item.totalAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. Budget vs Actual Execution
// -------------------------------------------------------------------------------------------------

@Composable
private fun BudgetVsActualReportCard(
    monthAllocations: List<CategoryAllocationUiModel>,
    totalAllocated: Double,
    unallocatedIncome: Double,
    plannedShortfall: Double,
    isShortfall: Boolean
) {
    val allocatedCategories = remember(monthAllocations) {
        monthAllocations.filter { it.allocatedAmount > 0 }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_budget_vs_actual_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Budget vs Actual",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Envelope target execution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${allocatedCategories.size} Envelopes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Summary row of allocations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Planned Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            text = CurrencyUtils.formatBDT(totalAllocated),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isShortfall) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(if (isShortfall) "Budget Shortfall" else "Unallocated Surplus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            text = if (isShortfall) "-${CurrencyUtils.formatBDT(plannedShortfall)}" else "+${CurrencyUtils.formatBDT(unallocatedIncome)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isShortfall) ExpenseRed else IncomeGreen
                        )
                    }
                }
            }

            if (allocatedCategories.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No category budget envelopes configured for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allocatedCategories.forEach { item ->
                        BudgetItemReportRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetItemReportRow(item: CategoryAllocationUiModel) {
    val usagePercent = (item.usagePercentage * 100).toInt()
    val progressClamped = item.usagePercentage.coerceIn(0f, 1f)
    val statusColor = when {
        item.isOverBudget -> ExpenseRed
        item.usagePercentage >= 0.85f -> Color(0xFFF59E0B)
        else -> IncomeGreen
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(item.category.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.category.icon,
                            contentDescription = null,
                            tint = item.category.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (item.isOverBudget) "Over ($usagePercent%)" else "$usagePercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressClamped },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${CurrencyUtils.formatBDT(item.actualSpent)} of ${CurrencyUtils.formatBDT(item.allocatedAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Text(
                    text = if (item.isOverBudget) "-${CurrencyUtils.formatBDT(abs(item.remainingAmount))}" else "${CurrencyUtils.formatBDT(item.remainingAmount)} left",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 7. Daily Spending Behavior & Limit Adherence
// -------------------------------------------------------------------------------------------------

@Composable
private fun DailySpendingAnalysisCard(
    currentMonthDays: List<CalendarDayCell>,
    dailyLimit: Double,
    totalExpense: Double
) {
    val spendingDays = remember(currentMonthDays) {
        currentMonthDays.filter { it.dayExpenseTotal > 0 }
    }
    val daysOverLimit = remember(currentMonthDays, dailyLimit) {
        currentMonthDays.count { it.dayExpenseTotal > dailyLimit }
    }
    val avgDailySpend = remember(spendingDays, totalExpense) {
        if (spendingDays.isNotEmpty()) totalExpense / spendingDays.size else 0.0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_daily_spending_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Daily Spending Adherence",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Compared against ${CurrencyUtils.formatBDT(dailyLimit)} limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (daysOverLimit == 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (daysOverLimit == 0) "100% Adherent" else "$daysOverLimit Over Days",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (daysOverLimit == 0) IncomeGreen else ExpenseRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Metric pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Active Spending Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            text = "${spendingDays.size} of ${currentMonthDays.size} days",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Average on Active Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            text = CurrencyUtils.formatBDT(avgDailySpend),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (avgDailySpend > dailyLimit) ExpenseRed else IncomeGreen
                        )
                    }
                }
            }

            if (currentMonthDays.isEmpty() || totalExpense <= 0.0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No daily expense records to display for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val maxExpenseInMonth = remember(currentMonthDays, dailyLimit) {
                    max(dailyLimit * 1.2, currentMonthDays.maxOfOrNull { it.dayExpenseTotal } ?: dailyLimit)
                }

                // Daily Bar Chart Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(vertical = 4.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 18.dp.toPx()
                    val chartHeight = height - bottomPadding
                    val daysCount = currentMonthDays.size

                    val slotWidth = width / daysCount
                    val barWidth = (slotWidth * 0.7f).coerceAtLeast(3.dp.toPx())

                    // Draw daily limit dashed line
                    val limitNormalized = (dailyLimit / maxExpenseInMonth).toFloat().coerceIn(0f, 1f)
                    val yLimit = chartHeight - (limitNormalized * chartHeight)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.6f),
                        start = Offset(0f, yLimit),
                        end = Offset(width, yLimit),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // Draw day bars
                    currentMonthDays.forEachIndexed { index, day ->
                        val xCenter = (index * slotWidth) + (slotWidth / 2f)
                        val xLeft = xCenter - (barWidth / 2f)
                        val expenseNormalized = (day.dayExpenseTotal / maxExpenseInMonth).toFloat().coerceIn(0f, 1f)
                        val barHeight = (expenseNormalized * chartHeight).coerceAtLeast(0f)
                        val yTop = chartHeight - barHeight

                        val isOver = day.dayExpenseTotal > dailyLimit
                        val barColor = if (isOver) ExpenseRed else IncomeGreen.copy(alpha = 0.75f)

                        if (barHeight > 0f) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(xLeft, yTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Day 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("Dashed line = Daily Limit (${CurrencyUtils.formatBDT(dailyLimit)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("Day ${currentMonthDays.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 8. Savings & Wealth Accumulation Analysis
// -------------------------------------------------------------------------------------------------

@Composable
private fun SavingsAnalysisReportCard(
    plannedSavings: Double,
    netOperatingCashChange: Double,
    totalIncome: Double
) {
    val savingsRate = remember(netOperatingCashChange, totalIncome) {
        if (totalIncome > 0) ((netOperatingCashChange.coerceAtLeast(0.0) / totalIncome) * 100).toInt() else 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_savings_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Savings & Wealth Retention",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Planned reserves vs actual surplus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = IncomeGreen.copy(alpha = 0.1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Planned Target Reserves", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            text = CurrencyUtils.formatBDT(plannedSavings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        Text("Savings + Emergency envelopes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (netOperatingCashChange >= 0) IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Actual Cash Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            text = CurrencyUtils.formatBDTWithSign(netOperatingCashChange, netOperatingCashChange >= 0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netOperatingCashChange >= 0) IncomeGreen else ExpenseRed
                        )
                        Text("Income minus expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Operating Savings Rate",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Percentage of income retained as cash",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "$savingsRate%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (savingsRate >= 20) IncomeGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 9. Debt & Loan Portfolio Analysis
// -------------------------------------------------------------------------------------------------

@Composable
private fun LoanPortfolioAnalysisCard(
    allLoans: List<LoanEntity>,
    activeLent: Double,
    activeBorrowed: Double
) {
    val activeLoans = remember(allLoans) {
        allLoans.filter { it.status == LoanStatus.ACTIVE }
    }
    val settledLoans = remember(allLoans) {
        allLoans.filter { it.status == LoanStatus.SETTLED }
    }
    val totalInitialLent = remember(allLoans) {
        allLoans.filter { it.type == LoanType.LENT }.sumOf { it.initialAmount }
    }
    val totalInitialBorrowed = remember(allLoans) {
        allLoans.filter { it.type == LoanType.BORROWED }.sumOf { it.initialAmount }
    }
    val totalRepaid = remember(totalInitialLent, activeLent, totalInitialBorrowed, activeBorrowed) {
        (totalInitialLent - activeLent).coerceAtLeast(0.0) + (totalInitialBorrowed - activeBorrowed).coerceAtLeast(0.0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_loan_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LoanBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = LoanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Debt & Loan Portfolio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Receivables, payables & repayments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${activeLoans.size} Active • ${settledLoans.size} Settled",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (allLoans.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active or settled loan records in the application.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = IncomeGreen.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("You Lent (Receivable)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text(
                                text = CurrencyUtils.formatBDT(activeLent),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = LoanBlue.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("You Owe (Payable)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text(
                                text = CurrencyUtils.formatBDT(activeBorrowed),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LoanBlue
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cumulative Repayments Recovered/Settled",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Total historical debt repayments to date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatBDT(totalRepaid),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
