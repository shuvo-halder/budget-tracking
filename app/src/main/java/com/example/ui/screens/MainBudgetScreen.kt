package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.CategorySpendChart
import com.example.ui.components.DashboardCard
import com.example.ui.components.FilterChipsBar
import com.example.ui.components.QuickActionsRow
import com.example.ui.components.SetBudgetDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyUtils
import com.example.ui.util.DateUtils
import com.example.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBudgetScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Confirm Delete Dialog
    if (transactionToDelete != null) {
        val target = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = {
                Text(
                    "Are you sure you want to delete this ${target.categoryName} entry of ${CurrencyUtils.formatBDT(target.amount)}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = target
                        transactionToDelete = null
                        viewModel.deleteTransaction(toDelete)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Transaction deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.saveTransaction(
                                    id = 0L,
                                    type = toDelete.type,
                                    amount = toDelete.amount,
                                    categoryId = toDelete.categoryId,
                                    categoryName = toDelete.categoryName,
                                    note = toDelete.note,
                                    timestamp = toDelete.timestamp
                                )
                            }
                        }
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_budget_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Daily Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Personal Expense Tracker",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openBudgetLimitDialog() },
                        modifier = Modifier.testTag("open_budget_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Monthly budget settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddSheet(TransactionType.EXPENSE) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new transaction"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dashboard at the top
            item(key = "dashboard_card") {
                DashboardCard(
                    balance = uiState.balance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    monthlyLimit = uiState.monthlyLimit,
                    monthlySpent = uiState.monthlySpent,
                    budgetProgress = uiState.budgetProgress,
                    onEditBudgetClick = { viewModel.openBudgetLimitDialog() }
                )
            }

            // 2. Prominent Quick Action Buttons
            item(key = "quick_actions_row") {
                QuickActionsRow(
                    onAddIncomeClick = { viewModel.openAddSheet(TransactionType.INCOME) },
                    onAddExpenseClick = { viewModel.openAddSheet(TransactionType.EXPENSE) }
                )
            }

            // 3. Category Spending Breakdown Chart (if expenses exist)
            if (uiState.categorySpendBreakdown.isNotEmpty()) {
                item(key = "category_spending_chart") {
                    CategorySpendChart(
                        breakdowns = uiState.categorySpendBreakdown,
                        totalExpense = uiState.totalExpense
                    )
                }
            }

            // 4. Filter & Search Controls
            item(key = "filter_bar") {
                FilterChipsBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedType = uiState.selectedTypeFilter,
                    onTypeSelect = { viewModel.setTypeFilter(it) },
                    selectedCategoryId = uiState.selectedCategoryFilterId,
                    onCategorySelect = { viewModel.setCategoryFilter(it) },
                    selectedDateOption = uiState.selectedDateFilter,
                    onDateOptionSelect = { viewModel.setDateFilter(it) }
                )
            }

            // 5. Recent Transactions List or Empty State
            if (uiState.filteredTransactions.isEmpty()) {
                item(key = "empty_state") {
                    EmptyTransactionsCard(
                        hasAnyTransactions = uiState.allTransactions.isNotEmpty(),
                        searchQuery = uiState.searchQuery,
                        onClearFilters = { viewModel.clearAllFilters() },
                        onAddExpense = { viewModel.openAddSheet(TransactionType.EXPENSE) }
                    )
                }
            } else {
                items(
                    items = uiState.filteredTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onClick = { viewModel.openEditSheet(transaction) },
                        onEditClick = { viewModel.openEditSheet(transaction) },
                        onDeleteClick = { transactionToDelete = transaction }
                    )
                }
            }
        }
    }

    // Modal Add / Edit Bottom Sheet
    AddEditTransactionSheet(
        isOpen = uiState.isAddEditSheetOpen,
        onDismiss = { viewModel.closeAddEditSheet() },
        editingTransaction = uiState.editingTransaction,
        defaultType = uiState.defaultSheetType,
        onSave = { id, type, amount, catId, catName, note, timestamp ->
            viewModel.saveTransaction(id, type, amount, catId, catName, note, timestamp)
        }
    )

    // Set Monthly Budget Dialog
    SetBudgetDialog(
        isOpen = uiState.isBudgetLimitDialogOpen,
        currentLimit = uiState.monthlyLimit,
        onDismiss = { viewModel.closeBudgetLimitDialog() },
        onConfirm = { newLimit ->
            viewModel.updateMonthlyBudgetLimit(newLimit)
        }
    )
}

@Composable
private fun EmptyTransactionsCard(
    hasAnyTransactions: Boolean,
    searchQuery: String,
    onClearFilters: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("empty_transactions_card"),
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
                text = if (hasAnyTransactions) {
                    "Try adjusting your category, type, or date filters."
                } else {
                    "Tap + Add Income or - Add Expense to record your first transaction."
                },
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
                    onClick = onAddExpense,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add First Item")
                }
            }
        }
    }
}
