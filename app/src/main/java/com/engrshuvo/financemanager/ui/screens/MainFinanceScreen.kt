package com.engrshuvo.financemanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.components.AddRepaymentDialog
import com.engrshuvo.financemanager.ui.components.BudgetPlanningDialog
import com.engrshuvo.financemanager.ui.components.DailyLimitDialog
import com.engrshuvo.financemanager.ui.components.DashboardOverviewView
import com.engrshuvo.financemanager.ui.components.DaySummarySheet
import com.engrshuvo.financemanager.ui.components.LoansScreen
import com.engrshuvo.financemanager.ui.components.ModernBottomNavigationBar
import com.engrshuvo.financemanager.ui.components.SettleLoanDialog
import com.engrshuvo.financemanager.ui.components.SetBudgetDialog
import com.engrshuvo.financemanager.ui.components.UniversalTransactionSheet
import com.engrshuvo.financemanager.ui.state.FinanceTab
import com.engrshuvo.financemanager.ui.state.MoreSubDestination
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModel
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
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = uiState.moreSubDestination != MoreSubDestination.NONE || uiState.activeTab != FinanceTab.DASHBOARD) {
        if (uiState.moreSubDestination != MoreSubDestination.NONE) {
            viewModel.navigateBackFromMoreSubDestination()
        } else {
            viewModel.setActiveTab(FinanceTab.DASHBOARD)
        }
    }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var loanToDelete by remember { mutableStateOf<LoanEntity?>(null) }
    var loanToSettle by remember { mutableStateOf<LoanEntity?>(null) }

    LaunchedEffect(uiState.loanOperationError) {
        val error = uiState.loanOperationError
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearLoanOperationError()
        }
    }

    // Soft delete confirmation dialog for transaction
    if (transactionToDelete != null) {
        val target = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Move to Archive?") },
            text = {
                Text("Archive ${target.categoryName} entry of ${CurrencyUtils.formatBDT(target.amount)}? It can be restored within 2 months before permanent auto-purge.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = target
                        transactionToDelete = null
                        viewModel.deleteTransaction(toDelete)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "${toDelete.categoryName} archived",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreTransaction(toDelete.id)
                            }
                        }
                    }
                ) {
                    Text("Archive", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Soft delete confirmation dialog for loan
    if (loanToDelete != null) {
        val targetLoan = loanToDelete!!
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = { Text("Archive Loan Record?") },
            text = {
                Text("Archive loan with ${targetLoan.personName} (${CurrencyUtils.formatBDT(targetLoan.initialAmount)}) and linked repayments? It can be restored within 2 months.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDeleteLoan = targetLoan
                        loanToDelete = null
                        viewModel.deleteLoan(toDeleteLoan)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Loan with ${toDeleteLoan.personName} archived",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreLoan(toDeleteLoan.id)
                            }
                        }
                    }
                ) {
                    Text("Archive", color = ExpenseRed, fontWeight = FontWeight.Bold)
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
            // Show main top bar when not in sub-screens or tabs with their own top bars
            if (uiState.moreSubDestination == MoreSubDestination.NONE && uiState.activeTab != FinanceTab.REPORTS) {
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
                                    text = when (uiState.activeTab) {
                                        FinanceTab.REPORTS -> "Financial Reports & Trends"
                                        FinanceTab.BUDGET -> "Monthly Envelope Planning"
                                        FinanceTab.DASHBOARD -> "Financial Dashboard"
                                        FinanceTab.LOANS -> "Debt & Loan Tracker"
                                        FinanceTab.MORE -> "More Features & Tools"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        when (uiState.activeTab) {
                            FinanceTab.DASHBOARD -> {
                                IconButton(
                                    onClick = { viewModel.navigateToMoreSubDestination(MoreSubDestination.CALENDAR) },
                                    modifier = Modifier.testTag("top_calendar_shortcut_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Open Full Calendar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            FinanceTab.BUDGET -> {
                                IconButton(
                                    onClick = { viewModel.openBudgetPlanningDialog() },
                                    modifier = Modifier.testTag("top_budget_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Budget settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            else -> {}
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            ModernBottomNavigationBar(
                activeTab = uiState.activeTab,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
        },
        floatingActionButton = {
            val showFab = uiState.moreSubDestination == MoreSubDestination.NONE &&
                    uiState.activeTab != FinanceTab.MORE &&
                    uiState.activeTab != FinanceTab.REPORTS
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        when (uiState.activeTab) {
                            FinanceTab.LOANS -> viewModel.openAddTransactionSheet(TransactionType.LOAN, LoanType.LENT)
                            FinanceTab.BUDGET -> viewModel.openBudgetPlanningDialog()
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
                        contentDescription = "Add new transaction, loan or budget"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (uiState.moreSubDestination != MoreSubDestination.NONE) {
            MoreScreen(
                uiState = uiState,
                onNavigateToSubDestination = { viewModel.navigateToMoreSubDestination(it) },
                onNavigateBack = { viewModel.navigateBackFromMoreSubDestination() },
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) },
                onResetToToday = { viewModel.resetToCurrentMonth() },
                onSelectDate = { timestamp -> viewModel.selectDate(timestamp, openSheet = true) },
                onEditTransaction = { viewModel.openEditTransactionSheet(it) },
                onDeleteTransaction = { transactionToDelete = it },
                onArchiveSearchQueryChange = { viewModel.setArchiveSearchQuery(it) },
                onArchiveFilterSelect = { viewModel.setArchiveFilterType(it) },
                onRestoreArchiveItem = { item ->
                    when (item) {
                        is com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper.Transaction -> {
                            viewModel.restoreTransaction(item.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Transaction restored")
                            }
                        }
                        is com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper.Loan -> {
                            viewModel.restoreLoan(item.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Loan and repayments restored")
                            }
                        }
                        is com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper.Goal -> {
                            viewModel.restoreGoal(item.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Goal restored")
                            }
                        }
                    }
                },
                onRequestPermanentDelete = { viewModel.openPermanentDeleteDialog(it) },
                onConfirmPermanentDelete = { viewModel.confirmPermanentDelete() },
                onDismissPermanentDeleteDialog = { viewModel.closePermanentDeleteDialog() },
                onUpdateMorningEnabled = { viewModel.updateMorningNotificationEnabled(it, context) },
                onUpdateMorningTime = { hour, minute -> viewModel.updateMorningNotificationTime(hour, minute, context) },
                onUpdateReminderEnabled = { viewModel.updateExpenseReminderEnabled(it, context) },
                onUpdateReminderInterval = { viewModel.updateExpenseReminderInterval(it, context) },
                onUpdateQuietHours = { enabled, start, end -> viewModel.updateQuietHours(enabled, start, end, context) },
                onExportBackup = { viewModel.exportBackupToUri(it, context) },
                onReadBackupFile = { viewModel.readBackupFile(it, context) },
                onConfirmRestore = { viewModel.confirmRestoreBackup(context) },
                onDismissRestorePreview = { viewModel.dismissRestorePreview() },
                onDismissOperationMessage = { viewModel.dismissBackupOperationMessage() },
                onOpenCreateGoalDialog = { viewModel.openCreateGoalDialog() },
                onOpenEditGoalDialog = { viewModel.openEditGoalDialog(it) },
                onDismissGoalDialog = { viewModel.dismissGoalDialog() },
                onSaveGoal = { name, cat, target, initial, targetDate, prio, monthly ->
                    viewModel.saveGoal(name, cat, target, initial, targetDate, prio, monthly)
                },
                onUpdateGoalStatus = { goal, status -> viewModel.updateGoalStatus(goal, status) },
                onArchiveGoal = { viewModel.archiveGoal(it) },
                onOpenAddContributionDialog = { viewModel.openAddContributionDialog(it) },
                onDismissAddContributionDialog = { viewModel.dismissAddContributionDialog() },
                onSaveGoalContribution = { goalId, amount, date, note ->
                    viewModel.saveGoalContribution(goalId, amount, date, note)
                },
                onDeleteGoalContribution = { viewModel.deleteGoalContribution(it) },
                onSetGoalFilterStatus = { viewModel.setGoalFilterStatus(it) },
                onUpdatePdfFilterState = { transform -> viewModel.updatePdfFilterState(transform) },
                onResetPdfFilterState = { viewModel.resetPdfFilterState() },
                onGeneratePdfToUri = { uri, ctx -> viewModel.generatePdfToUri(uri, ctx) },
                onSharePdfReport = { ctx -> viewModel.sharePdfReport(ctx) },
                onOpenPdfReport = { ctx, uri -> viewModel.openPdfReport(ctx, uri) },
                onDismissPdfMessage = { viewModel.dismissPdfExportMessage() },
                onSetPurchaseFilter = { viewModel.setPurchaseFilter(it) },
                onPurchaseSearchQueryChange = { viewModel.setPurchaseSearchQuery(it) },
                onOpenAddPurchaseItemDialog = { viewModel.openAddPurchaseItemDialog() },
                onOpenEditPurchaseItemDialog = { viewModel.openEditPurchaseItemDialog(it) },
                onDismissPurchaseItemDialog = { viewModel.closePurchaseItemDialog() },
                onSavePurchaseItem = { id, title, note -> viewModel.savePurchaseItem(id, title, note) },
                onTogglePurchaseItemComplete = { viewModel.togglePurchaseItemComplete(it) },
                onRestorePurchaseItem = { viewModel.restorePurchaseItem(it) },
                onRequestDeletePurchaseItem = { viewModel.requestDeletePurchaseItem(it) },
                onConfirmDeletePurchaseItem = { viewModel.confirmDeletePurchaseItem() },
                onDismissDeletePurchaseItemDialog = { viewModel.dismissDeletePurchaseItemDialog() },
                onToggleSmsEnabled = { enabled, ctx -> viewModel.updateSmsImportEnabled(enabled, ctx) },
                onToggleSmsAutoPost = { viewModel.updateSmsAutoPost(it) },
                onOpenReviewSmsRecord = { viewModel.openSmsRecordReview(it) },
                onDismissReviewSmsRecord = { viewModel.dismissSmsRecordReview() },
                onApproveSmsRecord = { id, catId, note -> viewModel.approveSmsRecord(id, catId, note) },
                onDismissSmsRecord = { viewModel.dismissSmsRecord(it) },
                onDeleteSmsRecord = { viewModel.deleteSmsRecord(it) },
                onSmsPasteInputChange = { viewModel.setSmsPasteInput(it) },
                onTestParseSmsPaste = { sender, body -> viewModel.testParseSmsPaste(sender, body) },
                onImportPastedSms = { ctx, sender, body -> viewModel.importPastedSms(ctx, sender, body) },
                onDismissSmsOperationMessage = { viewModel.dismissSmsOperationMessage() },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onTypeSelect = { viewModel.setTypeFilter(it) },
                onCategorySelect = { viewModel.setCategoryFilter(it) },
                onDateOptionSelect = { viewModel.setDateFilter(it) },
                onClearFilters = { viewModel.clearAllFilters() },
                onAddIncomeClick = { viewModel.openAddTransactionSheet(TransactionType.INCOME) },
                onAddExpenseClick = { viewModel.openAddTransactionSheet(TransactionType.EXPENSE) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            AnimatedContent(
                targetState = uiState.activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "tabContentAnim"
            ) { tab ->
                when (tab) {
                    FinanceTab.REPORTS -> {
                        ReportsScreen(
                            uiState = uiState,
                            onNavigateBack = { viewModel.setActiveTab(FinanceTab.DASHBOARD) },
                            onPreviousMonth = { viewModel.changeMonth(-1) },
                            onNextMonth = { viewModel.changeMonth(1) },
                            onResetToToday = { viewModel.resetToCurrentMonth() },
                            onViewTransactions = { viewModel.navigateToMoreSubDestination(MoreSubDestination.TRANSACTIONS) },
                            showBackButton = false
                        )
                    }

                    FinanceTab.BUDGET -> {
                        BudgetScreen(
                            monthName = DateUtils.formatMonthYear(uiState.displayedMonth),
                            totalMonthlyIncome = uiState.totalIncome,
                            totalAllocated = uiState.totalAllocated,
                            unallocatedIncome = uiState.unallocatedIncome,
                            plannedShortfall = uiState.plannedShortfall,
                            isShortfall = uiState.isShortfall,
                            plannedSavingsTotal = uiState.plannedSavingsTotal,
                            dailyLimit = uiState.dailyBudgetLimit,
                            todayExpenses = uiState.todayExpenses,
                            dailyRemaining = uiState.dailyBudgetRemaining,
                            dailyProgress = uiState.dailyBudgetProgress,
                            isDailyOverBudget = uiState.isDailyOverBudget,
                            allocations = uiState.monthAllocations,
                            isDraft = uiState.isBudgetAllocationDraft,
                            onPreviousMonth = { viewModel.changeMonth(-1) },
                            onNextMonth = { viewModel.changeMonth(1) },
                            onPlanBudgetClick = { viewModel.openBudgetPlanningDialog() },
                            onConfigureDailyLimitClick = { viewModel.openDailyLimitDialog() },
                            onCopyFromPreviousMonth = { viewModel.copyFromPreviousMonth() }
                        )
                    }

                    FinanceTab.DASHBOARD -> {
                        DashboardOverviewView(
                            monthName = DateUtils.formatMonthYear(uiState.displayedMonth),
                            onPreviousMonth = { viewModel.changeMonth(-1) },
                            onNextMonth = { viewModel.changeMonth(1) },
                            onResetToToday = { viewModel.resetToCurrentMonth() },
                            onOpenFullCalendar = { viewModel.navigateToMoreSubDestination(MoreSubDestination.CALENDAR) },
                            displayedMonth = uiState.displayedMonth,
                            calendarDays = uiState.calendarDays,
                            onDateClick = { timestamp -> viewModel.selectDate(timestamp, openSheet = false) },
                            selectedDateTimestamp = uiState.selectedDateTimestamp,
                            selectedDaySummary = uiState.selectedDaySummary,
                            selectedDayTransactions = uiState.selectedDayTransactions,
                            balance = uiState.balance,
                            totalIncome = uiState.totalIncome,
                            totalExpense = uiState.totalExpense,
                            netOperatingCashChange = uiState.netOperatingCashChange,
                            totalLent = uiState.totalActiveLent,
                            totalBorrowed = uiState.totalActiveBorrowed,
                            totalAllocated = uiState.totalAllocated,
                            unallocatedIncome = uiState.unallocatedIncome,
                            plannedShortfall = uiState.plannedShortfall,
                            isShortfall = uiState.isShortfall,
                            isDraft = uiState.isBudgetAllocationDraft,
                            plannedSavingsTotal = uiState.plannedSavingsTotal,
                            todayExpenses = uiState.todayExpenses,
                            dailyBudgetLimit = uiState.dailyBudgetLimit,
                            dailyBudgetRemaining = uiState.dailyBudgetRemaining,
                            dailyBudgetProgress = uiState.dailyBudgetProgress,
                            isDailyOverBudget = uiState.isDailyOverBudget,
                            monthAllocations = uiState.monthAllocations,
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
                            onPlanBudgetClick = { viewModel.openBudgetPlanningDialog() },
                            onConfigureDailyLimitClick = { viewModel.openDailyLimitDialog() },
                            onAddIncomeClick = { viewModel.openAddTransactionSheet(TransactionType.INCOME) },
                            onAddExpenseClick = { viewModel.openAddTransactionSheet(TransactionType.EXPENSE) },
                            onAddLoanClick = { viewModel.openAddTransactionSheet(TransactionType.LOAN, LoanType.LENT) },
                            onOpenSmsImport = { viewModel.navigateToMoreSubDestination(MoreSubDestination.SMS_IMPORT) },
                            onEditTransaction = { viewModel.openEditTransactionSheet(it) },
                            onDeleteTransaction = { transactionToDelete = it },
                            onViewTransactions = { viewModel.navigateToMoreSubDestination(MoreSubDestination.TRANSACTIONS) },
                            activeGoalsCount = uiState.activeGoals.size,
                            totalGoalSaved = uiState.totalGoalSaved,
                            totalGoalTarget = uiState.totalGoalTarget,
                            onOpenGoals = { viewModel.navigateToMoreSubDestination(MoreSubDestination.GOALS) }
                        )
                    }

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
                            onSettleClick = { loanToSettle = it },
                            onDeleteLoanClick = { loanToDelete = it }
                        )
                    }

                    FinanceTab.MORE -> {
                        MoreScreen(
                            uiState = uiState,
                            onNavigateToSubDestination = { viewModel.navigateToMoreSubDestination(it) },
                            onNavigateBack = { viewModel.setActiveTab(FinanceTab.DASHBOARD) },
                            onPreviousMonth = { viewModel.changeMonth(-1) },
                            onNextMonth = { viewModel.changeMonth(1) },
                            onResetToToday = { viewModel.resetToCurrentMonth() },
                            onSelectDate = { timestamp -> viewModel.selectDate(timestamp, openSheet = true) },
                            onEditTransaction = { viewModel.openEditTransactionSheet(it) },
                            onDeleteTransaction = { transactionToDelete = it },
                            onArchiveSearchQueryChange = { viewModel.setArchiveSearchQuery(it) },
                            onArchiveFilterSelect = { viewModel.setArchiveFilterType(it) },
                            onRestoreArchiveItem = { item ->
                                when (item) {
                                    is com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper.Transaction -> {
                                        viewModel.restoreTransaction(item.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Transaction restored")
                                        }
                                    }
                                    is com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper.Loan -> {
                                        viewModel.restoreLoan(item.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Loan and repayments restored")
                                        }
                                    }
                                    is com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper.Goal -> {
                                        viewModel.restoreGoal(item.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Goal restored")
                                        }
                                    }
                                }
                            },
                            onRequestPermanentDelete = { viewModel.openPermanentDeleteDialog(it) },
                            onConfirmPermanentDelete = { viewModel.confirmPermanentDelete() },
                            onDismissPermanentDeleteDialog = { viewModel.closePermanentDeleteDialog() },
                            onUpdateMorningEnabled = { viewModel.updateMorningNotificationEnabled(it, context) },
                            onUpdateMorningTime = { hour, minute -> viewModel.updateMorningNotificationTime(hour, minute, context) },
                            onUpdateReminderEnabled = { viewModel.updateExpenseReminderEnabled(it, context) },
                            onUpdateReminderInterval = { viewModel.updateExpenseReminderInterval(it, context) },
                            onUpdateQuietHours = { enabled, start, end -> viewModel.updateQuietHours(enabled, start, end, context) },
                            onExportBackup = { viewModel.exportBackupToUri(it, context) },
                            onReadBackupFile = { viewModel.readBackupFile(it, context) },
                            onConfirmRestore = { viewModel.confirmRestoreBackup(context) },
                            onDismissRestorePreview = { viewModel.dismissRestorePreview() },
                            onDismissOperationMessage = { viewModel.dismissBackupOperationMessage() },
                            onOpenCreateGoalDialog = { viewModel.openCreateGoalDialog() },
                            onOpenEditGoalDialog = { viewModel.openEditGoalDialog(it) },
                            onDismissGoalDialog = { viewModel.dismissGoalDialog() },
                            onSaveGoal = { name, cat, target, initial, targetDate, prio, monthly ->
                                viewModel.saveGoal(name, cat, target, initial, targetDate, prio, monthly)
                            },
                            onUpdateGoalStatus = { goal, status -> viewModel.updateGoalStatus(goal, status) },
                            onArchiveGoal = { viewModel.archiveGoal(it) },
                            onOpenAddContributionDialog = { viewModel.openAddContributionDialog(it) },
                            onDismissAddContributionDialog = { viewModel.dismissAddContributionDialog() },
                            onSaveGoalContribution = { goalId, amount, date, note ->
                                viewModel.saveGoalContribution(goalId, amount, date, note)
                            },
                            onDeleteGoalContribution = { viewModel.deleteGoalContribution(it) },
                            onSetGoalFilterStatus = { viewModel.setGoalFilterStatus(it) },
                            onUpdatePdfFilterState = { transform -> viewModel.updatePdfFilterState(transform) },
                            onResetPdfFilterState = { viewModel.resetPdfFilterState() },
                            onGeneratePdfToUri = { uri, ctx -> viewModel.generatePdfToUri(uri, ctx) },
                            onSharePdfReport = { ctx -> viewModel.sharePdfReport(ctx) },
                            onOpenPdfReport = { ctx, uri -> viewModel.openPdfReport(ctx, uri) },
                            onDismissPdfMessage = { viewModel.dismissPdfExportMessage() },
                            onSetPurchaseFilter = { viewModel.setPurchaseFilter(it) },
                            onPurchaseSearchQueryChange = { viewModel.setPurchaseSearchQuery(it) },
                            onOpenAddPurchaseItemDialog = { viewModel.openAddPurchaseItemDialog() },
                            onOpenEditPurchaseItemDialog = { viewModel.openEditPurchaseItemDialog(it) },
                            onDismissPurchaseItemDialog = { viewModel.closePurchaseItemDialog() },
                            onSavePurchaseItem = { id, title, note -> viewModel.savePurchaseItem(id, title, note) },
                            onTogglePurchaseItemComplete = { viewModel.togglePurchaseItemComplete(it) },
                            onRestorePurchaseItem = { viewModel.restorePurchaseItem(it) },
                            onRequestDeletePurchaseItem = { viewModel.requestDeletePurchaseItem(it) },
                            onConfirmDeletePurchaseItem = { viewModel.confirmDeletePurchaseItem() },
                            onDismissDeletePurchaseItemDialog = { viewModel.dismissDeletePurchaseItemDialog() },
                            onToggleSmsEnabled = { enabled, ctx -> viewModel.updateSmsImportEnabled(enabled, ctx) },
                            onToggleSmsAutoPost = { viewModel.updateSmsAutoPost(it) },
                            onOpenReviewSmsRecord = { viewModel.openSmsRecordReview(it) },
                            onDismissReviewSmsRecord = { viewModel.dismissSmsRecordReview() },
                            onApproveSmsRecord = { id, catId, note -> viewModel.approveSmsRecord(id, catId, note) },
                            onDismissSmsRecord = { viewModel.dismissSmsRecord(it) },
                            onDeleteSmsRecord = { viewModel.deleteSmsRecord(it) },
                            onSmsPasteInputChange = { viewModel.setSmsPasteInput(it) },
                            onTestParseSmsPaste = { sender, body -> viewModel.testParseSmsPaste(sender, body) },
                            onImportPastedSms = { ctx, sender, body -> viewModel.importPastedSms(ctx, sender, body) },
                            onDismissSmsOperationMessage = { viewModel.dismissSmsOperationMessage() },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onTypeSelect = { viewModel.setTypeFilter(it) },
                            onCategorySelect = { viewModel.setCategoryFilter(it) },
                            onDateOptionSelect = { viewModel.setDateFilter(it) },
                            onClearFilters = { viewModel.clearAllFilters() },
                            onAddIncomeClick = { viewModel.openAddTransactionSheet(TransactionType.INCOME) },
                            onAddExpenseClick = { viewModel.openAddTransactionSheet(TransactionType.EXPENSE) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isDayDetailSheetOpen && uiState.selectedDaySummary != null) {
        DaySummarySheet(
            isOpen = true,
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
    }

    if (uiState.isAddTransactionSheetOpen) {
        UniversalTransactionSheet(
            isOpen = true,
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
    }

    AddRepaymentDialog(
        isOpen = uiState.isRepayDialogOpen,
        loan = uiState.repayingLoan,
        onDismiss = { viewModel.closeRepayDialog() },
        onSubmitRepayment = { loanId, amount, note ->
            viewModel.submitLoanRepayment(loanId, amount, note)
        },
        isSubmitting = uiState.isRepaymentSaving,
        serverErrorMessage = uiState.loanOperationError
    )

    SettleLoanDialog(
        isOpen = loanToSettle != null,
        loan = loanToSettle,
        onDismiss = { loanToSettle = null },
        onSettleWithCash = { loanId ->
            viewModel.settleLoanWithCash(loanId)
        },
        onSettleWithoutCash = { loanId, reason ->
            viewModel.settleLoanWithoutCash(loanId, reason)
        }
    )

    SetBudgetDialog(
        isOpen = uiState.isBudgetLimitDialogOpen,
        currentLimit = uiState.monthlyLimit,
        onDismiss = { viewModel.closeBudgetLimitDialog() },
        onConfirm = { newLimit ->
            viewModel.updateMonthlyBudgetLimit(newLimit)
        }
    )

    if (uiState.isBudgetPlanningDialogOpen) {
        BudgetPlanningDialog(
            monthName = DateUtils.formatMonthYear(uiState.displayedMonth),
            totalMonthlyIncome = uiState.totalIncome,
            currentAllocations = uiState.monthAllocations,
            isDraft = uiState.isBudgetAllocationDraft,
            onDismiss = { viewModel.closeBudgetPlanningDialog() },
            onSaveAllocations = { allocations ->
                viewModel.saveCategoryAllocations(allocations)
            },
            onCopyFromPreviousMonth = {
                viewModel.copyFromPreviousMonth()
            }
        )
    }

    if (uiState.isDailyLimitDialogOpen) {
        DailyLimitDialog(
            currentLimit = uiState.dailyBudgetLimit,
            suggestedDailyLimit = uiState.suggestedDailyLimit,
            onDismiss = { viewModel.closeDailyLimitDialog() },
            onSaveLimit = { newDailyLimit ->
                viewModel.updateDailyBudgetLimit(newDailyLimit)
            }
        )
    }
}
