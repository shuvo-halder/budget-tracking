package com.engrshuvo.financemanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.ui.components.CategorySpendChart
import com.engrshuvo.financemanager.ui.components.InteractiveCalendarView
import com.engrshuvo.financemanager.ui.components.TransactionItemCard
import com.engrshuvo.financemanager.ui.state.ArchiveFilterType
import com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper
import com.engrshuvo.financemanager.ui.state.CategorySpending
import com.engrshuvo.financemanager.ui.state.FinanceUiState
import com.engrshuvo.financemanager.ui.state.MoreSubDestination
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.theme.LoanBlue
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils

@Composable
fun MoreScreen(
    uiState: FinanceUiState,
    onNavigateToSubDestination: (MoreSubDestination) -> Unit,
    onNavigateBack: () -> Unit,
    // Calendar actions
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    // Archive actions
    onArchiveSearchQueryChange: (String) -> Unit,
    onArchiveFilterSelect: (ArchiveFilterType) -> Unit,
    onRestoreArchiveItem: (ArchiveItemWrapper) -> Unit,
    onRequestPermanentDelete: (ArchiveItemWrapper) -> Unit,
    onConfirmPermanentDelete: () -> Unit,
    onDismissPermanentDeleteDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.moreSubDestination,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "moreScreenSubAnim",
        modifier = modifier.fillMaxSize()
    ) { destination ->
        when (destination) {
            MoreSubDestination.NONE -> {
                MoreHubView(
                    archivedCount = uiState.archivedItems.size,
                    onNavigateToSubDestination = onNavigateToSubDestination
                )
            }

            MoreSubDestination.CALENDAR -> {
                BackHandler { onNavigateBack() }
                MoreCalendarView(
                    uiState = uiState,
                    onNavigateBack = onNavigateBack,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onResetToToday = onResetToToday,
                    onSelectDate = onSelectDate,
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = onDeleteTransaction
                )
            }

            MoreSubDestination.ARCHIVE -> {
                BackHandler { onNavigateBack() }
                ArchiveScreen(
                    archivedItems = uiState.filteredArchivedItems,
                    searchQuery = uiState.archiveSearchQuery,
                    selectedFilter = uiState.archiveFilterType,
                    itemToPermanentlyDelete = uiState.itemToPermanentlyDelete,
                    onSearchQueryChange = onArchiveSearchQueryChange,
                    onFilterSelect = onArchiveFilterSelect,
                    onRestoreItem = onRestoreArchiveItem,
                    onRequestPermanentDelete = onRequestPermanentDelete,
                    onConfirmPermanentDelete = onConfirmPermanentDelete,
                    onDismissPermanentDeleteDialog = onDismissPermanentDeleteDialog,
                    onNavigateBack = onNavigateBack
                )
            }

            MoreSubDestination.REPORTS -> {
                BackHandler { onNavigateBack() }
                MoreReportsView(
                    uiState = uiState,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun MoreHubView(
    archivedCount: Int,
    onNavigateToSubDestination: (MoreSubDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen_hub"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "more_header") {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Tools & Services",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Calendar ledger, archived recovery system, and analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "more_item_calendar") {
            MoreDestinationCard(
                icon = Icons.Default.CalendarMonth,
                iconColor = MaterialTheme.colorScheme.primary,
                iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                title = "Interactive Calendar",
                subtitle = "Day-by-day expense timeline, dot indicators, and full 42-day calendar matrix",
                badgeText = "42-Day Matrix",
                badgeColor = MaterialTheme.colorScheme.primary,
                onClick = { onNavigateToSubDestination(MoreSubDestination.CALENDAR) },
                testTag = "more_calendar_card"
            )
        }

        item(key = "more_item_archive") {
            MoreDestinationCard(
                icon = Icons.Default.Archive,
                iconColor = Color(0xFFD97706),
                iconBg = Color(0xFFFEF3C7),
                title = "Archive & Recovery",
                subtitle = "Safely restored soft-deleted records, undo protection, and 2-month auto-purge",
                badgeText = if (archivedCount > 0) "$archivedCount archived" else "Safe trash",
                badgeColor = if (archivedCount > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.outline,
                onClick = { onNavigateToSubDestination(MoreSubDestination.ARCHIVE) },
                testTag = "more_archive_card"
            )
        }

        item(key = "more_item_reports") {
            MoreDestinationCard(
                icon = Icons.Default.Analytics,
                iconColor = Color(0xFF2563EB),
                iconBg = Color(0xFFDBEAFE),
                title = "Reports & Analytics",
                subtitle = "Category donut chart, monthly cash flow ratios, and planned savings reserve tracking",
                badgeText = "Insights",
                badgeColor = Color(0xFF2563EB),
                onClick = { onNavigateToSubDestination(MoreSubDestination.REPORTS) },
                testTag = "more_reports_card"
            )
        }

        item(key = "more_system_info") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Privacy & Data Protection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "• 100% Offline-first: All transactions, budgets, and loans are stored exclusively on your device in Room Database v3.\n• Deleting records preserves them in the Archive for 2 calendar months before permanent auto-purge.\n• Restoring brings records back into financial calculations without duplicate accounting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Currency: BDT (৳)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Room Schema v3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreDestinationCard(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreCalendarView(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_calendar_view")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Calendar & Daily Log",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to More"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "calendar_grid") {
                InteractiveCalendarView(
                    displayedMonth = uiState.displayedMonth,
                    calendarDays = uiState.calendarDays,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onResetToToday = onResetToToday,
                    onDateClick = onSelectDate
                )
            }

            if (uiState.selectedDaySummary != null) {
                item(key = "selected_day_header") {
                    val summary = uiState.selectedDaySummary
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
                                        text = summary.formattedDate,
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
                                        Text(CurrencyUtils.formatBDT(summary.totalIncome), fontWeight = FontWeight.Bold, color = IncomeGreen, fontSize = 13.sp)
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = ExpenseRed.copy(alpha = 0.12f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(CurrencyUtils.formatBDT(summary.totalExpense), fontWeight = FontWeight.Bold, color = ExpenseRed, fontSize = 13.sp)
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = LoanBlue.copy(alpha = 0.12f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Loan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(CurrencyUtils.formatBDT(summary.totalLoan), fontWeight = FontWeight.Bold, color = LoanBlue, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.selectedDayTransactions.isNotEmpty()) {
                    items(
                        items = uiState.selectedDayTransactions,
                        key = { "day_tx_${it.id}" },
                        contentType = { "day_transaction_item" }
                    ) { item ->
                        TransactionItemCard(
                            transaction = item,
                            onClick = { onEditTransaction(item) },
                            onEditClick = { onEditTransaction(item) },
                            onDeleteClick = { onDeleteTransaction(item) }
                        )
                    }
                } else {
                    item(key = "empty_day_txs") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "No financial activity recorded on this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreReportsView(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_reports_view")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Reports & Analytics",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to More"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month indicator
            item(key = "reports_month_header") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = DateUtils.formatMonthYear(uiState.displayedMonth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Monthly financial execution report",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Category Spend Chart
            item(key = "reports_donut_chart") {
                CategorySpendChart(
                    breakdowns = uiState.categorySpendBreakdown,
                    totalExpense = uiState.totalExpense
                )
            }

            // Planned Savings & Reserve Health
            item(key = "reports_reserves_card") {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Savings & Emergency Reserves",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monthly planned wealth accumulation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = IncomeGreen.copy(alpha = 0.1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Planned Savings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyUtils.formatBDT(uiState.plannedSavingsTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.isShortfall) ExpenseRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (uiState.isShortfall) "Budget Shortfall" else "Surplus Buffer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (uiState.isShortfall) "-${CurrencyUtils.formatBDT(uiState.plannedShortfall)}" else "+${CurrencyUtils.formatBDT(uiState.unallocatedIncome)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isShortfall) ExpenseRed else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cash Flow Summary
            item(key = "reports_cash_flow") {
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
                        Text(
                            text = "Cash Flow Ratios",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        val savingsRate = if (uiState.totalIncome > 0) {
                            ((uiState.netOperatingCashChange.coerceAtLeast(0.0) / uiState.totalIncome) * 100).toInt()
                        } else 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Operating Savings Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$savingsRate%", fontWeight = FontWeight.Bold, color = if (savingsRate >= 20) IncomeGreen else MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Net Operating Cash Change", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val isNetPos = uiState.netOperatingCashChange >= 0
                            Text(
                                text = (if (isNetPos) "+" else "") + CurrencyUtils.formatBDT(uiState.netOperatingCashChange),
                                fontWeight = FontWeight.Bold,
                                color = if (isNetPos) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }
        }
    }
}
