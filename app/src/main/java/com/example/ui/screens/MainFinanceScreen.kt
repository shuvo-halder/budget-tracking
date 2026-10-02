package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LoanEntity
import com.example.data.model.LoanType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.AddRepaymentDialog
import com.example.ui.components.DashboardOverviewView
import com.example.ui.components.DaySummarySheet
import com.example.ui.components.InteractiveCalendarView
import com.example.ui.components.LoansScreen
import com.example.ui.components.SetBudgetDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.UniversalTransactionSheet
import com.example.ui.state.FinanceTab
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyUtils
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFinanceScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var loanToDelete by remember { mutableStateOf<LoanEntity?>(null) }

    // Transaction Delete Dialog
    if (transactionToDelete != null) {
        val target = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = {
                Text("Are you sure you want to delete ${target.categoryName} entry of ${CurrencyUtils.formatBDT(target.amount)}?")
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

    // Loan Delete Dialog
    if (loanToDelete != null) {
        val targetLoan = loanToDelete!!
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = { Text("Delete Loan Record") },
            text = {
                Text("Are you sure you want to delete the loan with ${targetLoan.personName} (${CurrencyUtils.formatBDT(targetLoan.initialAmount)})?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLoan(targetLoan)
                        loanToDelete = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Loan record deleted")
                        }
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_finance_screen"),
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
                                text = "Daily Finance & Loans",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (uiState.activeTab) {
                                    FinanceTab.CALENDAR -> "Calendar & Daily Log"
                                    FinanceTab.DASHBOARD -> "Financial Dashboard"
                                    FinanceTab.LOANS -> "Debt & Loan Tracker"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openBudgetLimitDialog() },
                        modifier = Modifier.testTag("top_budget_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Budget settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                FinanceTab.values().forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    val icon = when (tab) {
                        FinanceTab.CALENDAR -> Icons.Default.CalendarMonth
                        FinanceTab.DASHBOARD -> Icons.Default.Dashboard
                        FinanceTab.LOANS -> Icons.Default.Handshake
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(imageVector = icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (uiState.activeTab) {
                        FinanceTab.LOANS -> viewModel.openAddTransactionSheet(TransactionType.LOAN, LoanType.LENT)
                        else -> viewModel.openAddTransactionSheet(TransactionType.EXPENSE)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("universal_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new transaction or loan"
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.activeTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tabContentAnim"
        ) { tab ->
            when (tab) {
                // 1. Calendar View
                FinanceTab.CALENDAR -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("calendar_tab_view"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Monthly Interactive Calendar Grid
                        item(key = "calendar_grid") {
                            InteractiveCalendarView(
                                displayedMonth = uiState.displayedMonth,
                                calendarDays = uiState.calendarDays,
                                onPreviousMonth = { viewModel.changeMonth(-1) },
                                onNextMonth = { viewModel.changeMonth(1) },
                                onResetToToday = { viewModel.resetToCurrentMonth() },
                                onDateClick = { timestamp -> viewModel.selectDate(timestamp, openSheet = true) }
                            )
                        }

                        // Selected Day Breakdown Card
                        if (uiState.selectedDaySummary != null) {
                            item(key = "selected_day_header") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = uiState.selectedDaySummary?.formattedDate ?: "",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Daily Total Activity",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "${uiState.selectedDayTransactions.size} entries",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                color = IncomeGreen.copy(alpha = 0.12f)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(CurrencyUtils.formatBDT(uiState.selectedDaySummary?.totalIncome ?: 0.0), fontWeight = FontWeight.Bold, color = IncomeGreen, fontSize = 13.sp)
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                color = ExpenseRed.copy(alpha = 0.12f)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(CurrencyUtils.formatBDT(uiState.selectedDaySummary?.totalExpense ?: 0.0), fontWeight = FontWeight.Bold, color = ExpenseRed, fontSize = 13.sp)
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Loan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(CurrencyUtils.formatBDT(uiState.selectedDaySummary?.totalLoan ?: 0.0), fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6), fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Selected day transaction items
                            if (uiState.selectedDayTransactions.isNotEmpty()) {
                                items(
                                    items = uiState.selectedDayTransactions,
                                    key = { "day_${it.id}" }
                                ) { item ->
                                    TransactionItemCard(
                                        transaction = item,
                                        onClick = { viewModel.openEditTransactionSheet(item) },
                                        onEditClick = { viewModel.openEditTransactionSheet(item) },
                                        onDeleteClick = { transactionToDelete = item }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Dashboard View
                FinanceTab.DASHBOARD -> {
                    DashboardOverviewView(
                        balance = uiState.balance,
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense,
                        totalLent = uiState.totalActiveLent,
                        totalBorrowed = uiState.totalActiveBorrowed,
                        monthlyLimit = uiState.monthlyLimit,
                        monthlySpent = uiState.monthlySpent,
                        budgetProgress = uiState.budgetProgress,
                        categoryBreakdown = uiState.categorySpendBreakdown,
                        filteredTransactions = uiState.filteredTransactions,
                        hasAnyTransactions = uiState.allTransactions.isNotEmpty(),
                        searchQuery = uiState.searchQuery,
                        selectedType = uiState.selectedTypeFilter,
                        selectedCategoryId = uiState.selectedCategoryFilterId,
                        selectedDateOption = uiState.selectedDateFilter,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onTypeSelect = { viewModel.setTypeFilter(it) },
                        onCategorySelect = { viewModel.setCategoryFilter(it) },
                        onDateOptionSelect = { viewModel.setDateFilter(it) },
                        onClearFilters = { viewModel.clearAllFilters() },
                        onEditBudgetClick = { viewModel.openBudgetLimitDialog() },
                        onAddIncomeClick = { viewModel.openAddTransactionSheet(TransactionType.INCOME) },
                        onAddExpenseClick = { viewModel.openAddTransactionSheet(TransactionType.EXPENSE) },
                        onEditTransaction = { viewModel.openEditTransactionSheet(it) },
                        onDeleteTransaction = { transactionToDelete = it }
                    )
                }

                // 3. Loans View
                FinanceTab.LOANS -> {
                    LoansScreen(
                        loans = uiState.filteredLoans,
                        totalLent = uiState.totalActiveLent,
                        totalBorrowed = uiState.totalActiveBorrowed,
                        selectedTypeFilter = uiState.selectedLoanFilter,
                        onTypeFilterChange = { viewModel.setLoanTypeFilter(it) },
                        selectedStatusFilter = uiState.selectedLoanStatusFilter,
                        onStatusFilterChange = { viewModel.setLoanStatusFilter(it) },
                        onAddLoanClick = { viewModel.openAddTransactionSheet(TransactionType.LOAN, LoanType.LENT) },
                        onRepayClick = { viewModel.openRepayDialog(it) },
                        onSettleClick = { viewModel.markLoanSettled(it.id) },
                        onDeleteLoanClick = { loanToDelete = it }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet: Selected Day Breakdown
    DaySummarySheet(
        isOpen = uiState.isDayDetailSheetOpen,
        onDismiss = { viewModel.closeDayDetailSheet() },
        daySummary = uiState.selectedDaySummary,
        transactions = uiState.selectedDayTransactions,
        onAddEntryForDay = {
            viewModel.closeDayDetailSheet()
            viewModel.openAddTransactionSheet(TransactionType.EXPENSE)
        },
        onEditTransaction = {
            viewModel.closeDayDetailSheet()
            viewModel.openEditTransactionSheet(it)
        },
        onDeleteTransaction = { transactionToDelete = it }
    )

    // Modal Bottom Sheet: Universal FAB Entry (Income / Expense / Loan)
    UniversalTransactionSheet(
        isOpen = uiState.isAddTransactionSheetOpen,
        onDismiss = { viewModel.closeAddTransactionSheet() },
        editingTransaction = uiState.editingTransaction,
        defaultEntryType = uiState.defaultEntryType,
        defaultLoanType = uiState.defaultLoanType,
        onSaveTransaction = { id, type, amount, catId, catName, note, timestamp ->
            viewModel.saveTransaction(id, type, amount, catId, catName, note, timestamp)
        },
        onSaveLoan = { type, personName, phone, amount, startDate, dueDate, note ->
            viewModel.saveLoanTransaction(type, personName, phone, amount, startDate, dueDate, note)
        }
    )

    // Dialog: Record Loan Repayment
    AddRepaymentDialog(
        isOpen = uiState.isRepayDialogOpen,
        loan = uiState.repayingLoan,
        onDismiss = { viewModel.closeRepayDialog() },
        onSubmitRepayment = { loanId, amount, note ->
            viewModel.submitLoanRepayment(loanId, amount, note)
        }
    )

    // Dialog: Set Monthly Spending Budget
    SetBudgetDialog(
        isOpen = uiState.isBudgetLimitDialogOpen,
        currentLimit = uiState.monthlyLimit,
        onDismiss = { viewModel.closeBudgetLimitDialog() },
        onConfirm = { newLimit ->
            viewModel.updateMonthlyBudgetLimit(newLimit)
        }
    )
}
