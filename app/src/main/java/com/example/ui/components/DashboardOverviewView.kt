package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.state.CategorySpending
import com.example.ui.state.DateFilterOption
import com.example.ui.state.TransactionTypeFilter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyUtils
import com.example.ui.util.DateUtils

@Composable
fun DashboardOverviewView(
    balance: Double,
    totalIncome: Double,
    totalExpense: Double,
    totalLent: Double,
    totalBorrowed: Double,
    monthlyLimit: Double,
    monthlySpent: Double,
    budgetProgress: Float,
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
    onEditBudgetClick: () -> Unit,
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
        // 1. Comprehensive Hero Balance & Stats Dashboard
        item(key = "dashboard_hero_card") {
            ComprehensiveDashboardCard(
                balance = balance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                totalLent = totalLent,
                totalBorrowed = totalBorrowed,
                monthlyLimit = monthlyLimit,
                monthlySpent = monthlySpent,
                budgetProgress = budgetProgress,
                onEditBudgetClick = onEditBudgetClick
            )
        }

        // 2. Quick Action Buttons (+ Add Income, - Add Expense)
        item(key = "quick_actions_row") {
            QuickActionsRow(
                onAddIncomeClick = onAddIncomeClick,
                onAddExpenseClick = onAddExpenseClick
            )
        }

        // 3. Category Spending Breakdown Chart
        if (categoryBreakdown.isNotEmpty()) {
            item(key = "category_spending_chart") {
                CategorySpendChart(
                    breakdowns = categoryBreakdown,
                    totalExpense = totalExpense
                )
            }
        }

        // 4. Search & Filter Bar
        item(key = "filter_bar") {
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

        // 5. Filtered Transaction Items or Empty Card
        if (filteredTransactions.isEmpty()) {
            item(key = "empty_state") {
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
                            text = if (hasAnyTransactions) "Try adjusting your category, type, or date filters." else "Tap + Add Income or - Add Expense to record your first transaction.",
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
                                Text("Add First Entry")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredTransactions,
                key = { it.id }
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
private fun ComprehensiveDashboardCard(
    balance: Double,
    totalIncome: Double,
    totalExpense: Double,
    totalLent: Double,
    totalBorrowed: Double,
    monthlyLimit: Double,
    monthlySpent: Double,
    budgetProgress: Float,
    onEditBudgetClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Balance Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                                text = "Current Balance",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = CurrencyUtils.formatBDT(balance, includeDecimalsIfZero = true),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Row 1: Total Income & Total Expense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiniStatPill(
                            title = "Total Income",
                            amount = totalIncome,
                            color = IncomeGreen,
                            icon = Icons.Default.ArrowDownward,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatPill(
                            title = "Total Expense",
                            amount = totalExpense,
                            color = ExpenseRed,
                            icon = Icons.Default.ArrowUpward,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Total Active Loans (You are Owed vs You Owe)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiniStatPill(
                            title = "You Lent (Owed)",
                            amount = totalLent,
                            color = Color(0xFF10B981),
                            icon = Icons.Default.Handshake,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatPill(
                            title = "You Owe (Debts)",
                            amount = totalBorrowed,
                            color = Color(0xFF3B82F6),
                            icon = Icons.Default.Handshake,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Monthly Budget Limit Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditBudgetClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            text = "${DateUtils.getCurrentMonthName()} Spending Target",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${CurrencyUtils.formatBDT(monthlySpent)} of ${CurrencyUtils.formatBDT(monthlyLimit)} spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val percentageInt = (budgetProgress * 100).toInt()
                        val progColor = when {
                            budgetProgress >= 1f -> ExpenseRed
                            budgetProgress >= 0.8f -> Color(0xFFF59E0B)
                            else -> IncomeGreen
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = progColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$percentageInt%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = progColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(onClick = onEditBudgetClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit budget target",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { budgetProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (budgetProgress >= 1f) ExpenseRed else if (budgetProgress >= 0.8f) Color(0xFFF59E0B) else IncomeGreen,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun MiniStatPill(
    title: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
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
                    text = CurrencyUtils.formatBDT(amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
