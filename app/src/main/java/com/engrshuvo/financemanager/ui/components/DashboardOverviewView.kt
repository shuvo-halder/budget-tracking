package com.engrshuvo.financemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import com.engrshuvo.financemanager.ui.state.CategorySpending
import com.engrshuvo.financemanager.ui.state.DateFilterOption
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.theme.LoanBlue
import com.engrshuvo.financemanager.ui.util.CurrencyUtils

@Composable
fun DashboardOverviewView(
    monthName: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    balance: Double,
    totalIncome: Double,
    totalExpense: Double,
    netOperatingCashChange: Double,
    totalLent: Double,
    totalBorrowed: Double,
    totalAllocated: Double,
    unallocatedIncome: Double,
    plannedShortfall: Double,
    isShortfall: Boolean,
    plannedSavingsTotal: Double,
    todayExpenses: Double,
    dailyBudgetLimit: Double,
    dailyBudgetRemaining: Double,
    dailyBudgetProgress: Float,
    isDailyOverBudget: Boolean,
    monthAllocations: List<CategoryAllocationUiModel>,
    categoryBreakdown: List<CategorySpending>,
    filteredTransactions: List<TransactionEntity>,
    hasAnyTransactions: Boolean,
    searchQuery: String,
    selectedType: TransactionTypeFilter,
    selectedCategoryId: String?,
    selectedDateOption: DateFilterOption,
    onSearchQueryChange: (String) -> Unit,
    onTypeSelect: (TransactionTypeFilter) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onDateOptionSelect: (DateFilterOption) -> Unit,
    onClearFilters: () -> Unit,
    onPlanBudgetClick: () -> Unit,
    onConfigureDailyLimitClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_overview_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Month Selector Header
        item(key = "month_selector_header", contentType = "month_selector") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.testTag("dashboard_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month"
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.testTag("dashboard_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month"
                        )
                    }
                }
            }
        }

        // 2. Planned Shortfall Alert Banner (Prominently displayed when allocations exceed income)
        if (isShortfall) {
            item(key = "planned_shortfall_banner", contentType = "shortfall_banner") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseRed.copy(alpha = 0.12f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(ExpenseRed, ExpenseRed.copy(alpha = 0.5f)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Budget Warning",
                                tint = ExpenseRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Planned Budget Shortfall",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Text(
                                text = "Planned allocations (${CurrencyUtils.formatBDT(totalAllocated)}) exceed received income (${CurrencyUtils.formatBDT(totalIncome)}) by ${CurrencyUtils.formatBDT(plannedShortfall)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }

                        TextButton(
                            onClick = onPlanBudgetClick,
                            modifier = Modifier.testTag("adjust_budget_banner_button")
                        ) {
                            Text("Adjust", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Section A: Monthly Financial Overview Hero Card
        item(key = "dashboard_hero_card", contentType = "hero_card") {
            SalaryFinancialHeroCard(
                balance = balance,
                monthName = monthName,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netOperatingCashChange = netOperatingCashChange,
                totalAllocated = totalAllocated,
                unallocatedIncome = unallocatedIncome,
                totalLent = totalLent,
                totalBorrowed = totalBorrowed,
                onPlanBudgetClick = onPlanBudgetClick
            )
        }

        // 4. Section B: Today's Spending Card
        item(key = "todays_spending_card", contentType = "today_spending") {
            TodaysSpendingCard(
                todayExpenses = todayExpenses,
                dailyLimit = dailyBudgetLimit,
                dailyRemaining = dailyBudgetRemaining,
                progress = dailyBudgetProgress,
                isOverBudget = isDailyOverBudget,
                onConfigureDailyLimitClick = onConfigureDailyLimitClick
            )
        }

        // 5. Quick Actions Row (+ Salary/Income, - Expense)
        item(key = "quick_actions_row", contentType = "quick_actions") {
            QuickActionsRow(
                onAddIncomeClick = onAddIncomeClick,
                onAddExpenseClick = onAddExpenseClick
            )
        }

        // 6. Section C: Monthly Budget Allocations (Rent, Family, Daily Expenses, etc.)
        item(key = "monthly_allocations_section", contentType = "allocations_section") {
            MonthlyBudgetCategorySection(
                monthName = monthName,
                allocations = monthAllocations,
                totalAllocated = totalAllocated,
                plannedSavingsTotal = plannedSavingsTotal,
                onPlanBudgetClick = onPlanBudgetClick
            )
        }

        // 7. Section E: Category Spending Distribution Donut Chart
        if (categoryBreakdown.isNotEmpty()) {
            item(key = "category_spending_chart", contentType = "donut_chart") {
                CategorySpendChart(
                    breakdowns = categoryBreakdown,
                    totalExpense = totalExpense
                )
            }
        }

        // 8. Section D: Filter Bar & Recent Transactions
        item(key = "filter_bar", contentType = "filter_bar") {
            Column {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FilterChipsBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    selectedType = selectedType,
                    onTypeSelect = onTypeSelect,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelect = onCategorySelect,
                    selectedDateOption = selectedDateOption,
                    onDateOptionSelect = onDateOptionSelect
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            item(key = "empty_state", contentType = "empty_placeholder") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasAnyTransactions) Icons.Default.FilterAltOff else Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = if (hasAnyTransactions) "No matching transactions" else "No transactions yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (hasAnyTransactions) "Try adjusting your category, type, or date filters." else "Tap + Income to log salary, or - Expense for daily spending.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (hasAnyTransactions) {
                            OutlinedButton(
                                onClick = onClearFilters,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reset Filters")
                            }
                        } else {
                            OutlinedButton(
                                onClick = onAddExpenseClick,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Expense")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredTransactions,
                key = { "tx_${it.id}" },
                contentType = { "transaction_card" }
            ) { transaction ->
                TransactionItemCard(
                    transaction = transaction,
                    onClick = { onEditTransaction(transaction) },
                    onEditClick = { onEditTransaction(transaction) },
                    onDeleteClick = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
private fun SalaryFinancialHeroCard(
    balance: Double,
    monthName: String,
    totalIncome: Double,
    totalExpense: Double,
    netOperatingCashChange: Double,
    totalAllocated: Double,
    unallocatedIncome: Double,
    totalLent: Double,
    totalBorrowed: Double,
    onPlanBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Available Cash Balance",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "BDT (৳)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = CurrencyUtils.formatBDT(balance, includeDecimalsIfZero = true),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Monthly Cash Flow Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatPill(
                        title = "$monthName Income",
                        amount = totalIncome,
                        color = IncomeGreen,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatPill(
                        title = "$monthName Expenses",
                        amount = totalExpense,
                        color = ExpenseRed,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Net cash change & Planned allocations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Net Operating Cash",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatBDTWithSign(netOperatingCashChange, isIncome = netOperatingCashChange >= 0),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (netOperatingCashChange >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Planned Allocations",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatBDT(totalAllocated),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Loan summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatPill(
                        title = "You Lent (Receivable)",
                        amount = totalLent,
                        color = IncomeGreen,
                        icon = Icons.Default.Handshake,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatPill(
                        title = "You Owe (Payable)",
                        amount = totalBorrowed,
                        color = LoanBlue,
                        icon = Icons.Default.Handshake,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaysSpendingCard(
    todayExpenses: Double,
    dailyLimit: Double,
    dailyRemaining: Double,
    progress: Float,
    isOverBudget: Boolean,
    onConfigureDailyLimitClick: () -> Unit
) {
    val progColor = when {
        isOverBudget -> ExpenseRed
        progress >= 0.8f -> Color(0xFFF59E0B)
        else -> IncomeGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Today's Living Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isOverBudget) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ExpenseRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Over Limit",
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (dailyLimit > 0) {
                            "${CurrencyUtils.formatBDT(todayExpenses)} of ${CurrencyUtils.formatBDT(dailyLimit)} spent"
                        } else {
                            "${CurrencyUtils.formatBDT(todayExpenses)} spent (no limit set)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onConfigureDailyLimitClick,
                    modifier = Modifier.testTag("configure_daily_limit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Daily Limit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (dailyLimit > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isOverBudget) "Overspent Today:" else "Remaining Today:",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatBDT(Math.abs(dailyRemaining)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) ExpenseRed else IncomeGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyBudgetCategorySection(
    monthName: String,
    allocations: List<CategoryAllocationUiModel>,
    totalAllocated: Double,
    plannedSavingsTotal: Double,
    onPlanBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Budget Allocations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Allocated: ${CurrencyUtils.formatBDT(totalAllocated)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onPlanBudgetClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("plan_budget_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Plan Budget", fontSize = 12.sp)
                }
            }

            if (plannedSavingsTotal > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Planned Savings & Reserves: ${CurrencyUtils.formatBDT(plannedSavingsTotal)} (not an expense)",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allocations.forEach { model ->
                    CategoryAllocationRow(model = model)
                }
            }
        }
    }
}

@Composable
private fun CategoryAllocationRow(model: CategoryAllocationUiModel) {
    val cat = model.category
    val isOver = model.isOverBudget
    val isAllocated = model.allocatedAmount > 0
    val progressClamped = model.usagePercentage.coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(cat.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        tint = cat.color,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isOver) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ExpenseRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Over Budget",
                                    color = ExpenseRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isAllocated) {
                            "${CurrencyUtils.formatBDT(model.actualSpent)} spent of ${CurrencyUtils.formatBDT(model.allocatedAmount)}"
                        } else {
                            "${CurrencyUtils.formatBDT(model.actualSpent)} spent (unallocated)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isAllocated) {
                            if (isOver) "-${CurrencyUtils.formatBDT(Math.abs(model.remainingAmount))}" else CurrencyUtils.formatBDT(model.remainingAmount)
                        } else {
                            "৳0"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isAllocated) "${(model.usagePercentage * 100).toInt()}% used" else "No target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            if (isAllocated) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressClamped },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isOver) ExpenseRed else cat.color,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAddIncomeClick,
            modifier = Modifier
                .weight(1f)
                .testTag("add_income_button"),
            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "+ Salary / Income",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = onAddExpenseClick,
            modifier = Modifier
                .weight(1f)
                .testTag("add_expense_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "- Expense",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MiniStatPill(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val formattedAmount by remember(amount) {
        derivedStateOf { CurrencyUtils.formatBDT(amount) }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
