package com.engrshuvo.financemanager.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.FinancialGoalUiModel
import com.engrshuvo.financemanager.data.model.GoalCategory
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import com.engrshuvo.financemanager.data.model.GoalPriority
import com.engrshuvo.financemanager.data.model.GoalStatus
import com.engrshuvo.financemanager.ui.state.FinanceUiState
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.theme.LoanBlue
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialGoalsScreen(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onOpenCreateGoalDialog: () -> Unit,
    onOpenEditGoalDialog: (FinancialGoalEntity) -> Unit,
    onDismissGoalDialog: () -> Unit,
    onSaveGoal: (
        name: String,
        category: GoalCategory,
        targetAmount: Double,
        initialSavedAmount: Double,
        targetDate: Long?,
        priority: GoalPriority,
        targetMonthlyContribution: Double?
    ) -> Unit,
    onUpdateGoalStatus: (FinancialGoalEntity, GoalStatus) -> Unit,
    onArchiveGoal: (Long) -> Unit,
    onOpenAddContributionDialog: (FinancialGoalEntity) -> Unit,
    onDismissAddContributionDialog: () -> Unit,
    onSaveGoalContribution: (goalId: Long, amount: Double, date: Long, note: String) -> Unit,
    onDeleteGoalContribution: (Long) -> Unit,
    onSetGoalFilterStatus: (GoalStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var expandedHistoryGoalId by remember { mutableStateOf<Long?>(null) }
    var goalToDeleteConfirmation by remember { mutableStateOf<FinancialGoalEntity?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("financial_goals_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Financial Goals",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        val activeCount = uiState.activeGoals.size
                        Text(
                            text = if (activeCount == 1) "1 active target" else "$activeCount active targets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("goals_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCreateGoalDialog,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Goal", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_goal_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Analytics Card
            item(key = "goals_summary_header") {
                GoalsSummaryAnalyticsCard(
                    totalTarget = uiState.totalGoalTarget,
                    totalSaved = uiState.totalGoalSaved,
                    overallProgressPercent = uiState.overallGoalProgressPercent,
                    thisMonthSaved = uiState.thisMonthGoalSaved,
                    totalMonthlyRequired = uiState.totalMonthlyGoalRequired,
                    savingCapacity = uiState.monthlyGoalSavingCapacity,
                    isDeficit = uiState.isGoalCapacityDeficit,
                    capacityDiff = uiState.goalCapacityDifference
                )
            }

            // Status Filter Chips Bar
            item(key = "goals_filter_chips") {
                GoalFilterChipsBar(
                    selectedStatus = uiState.selectedGoalFilterStatus,
                    onStatusSelect = onSetGoalFilterStatus,
                    allCount = uiState.allGoals.size,
                    activeCount = uiState.allGoals.count { it.goal.status == GoalStatus.ACTIVE },
                    completedCount = uiState.allGoals.count { it.goal.status == GoalStatus.COMPLETED },
                    pausedCount = uiState.allGoals.count { it.goal.status == GoalStatus.PAUSED }
                )
            }

            // Goals List or Empty State
            if (uiState.allGoals.isEmpty()) {
                item(key = "goals_empty_state") {
                    GoalsEmptyState(
                        filterStatus = uiState.selectedGoalFilterStatus,
                        onCreateGoal = onOpenCreateGoalDialog
                    )
                }
            } else {
                items(
                    items = uiState.allGoals,
                    key = { "goal_${it.goal.id}" }
                ) { goalModel ->
                    GoalItemCard(
                        goalModel = goalModel,
                        isHistoryExpanded = expandedHistoryGoalId == goalModel.goal.id,
                        onToggleHistory = {
                            expandedHistoryGoalId = if (expandedHistoryGoalId == goalModel.goal.id) null else goalModel.goal.id
                        },
                        onAddContribution = { onOpenAddContributionDialog(goalModel.goal) },
                        onEditGoal = { onOpenEditGoalDialog(goalModel.goal) },
                        onUpdateStatus = { newStatus -> onUpdateGoalStatus(goalModel.goal, newStatus) },
                        onArchiveGoal = { goalToDeleteConfirmation = goalModel.goal },
                        onDeleteContribution = onDeleteGoalContribution
                    )
                }
            }
        }
    }

    // Create / Edit Goal Dialog
    if (uiState.isCreateGoalDialogOpen) {
        CreateEditGoalDialog(
            editingGoal = uiState.editingGoal,
            onDismiss = onDismissGoalDialog,
            onSave = onSaveGoal
        )
    }

    // Add Contribution Dialog
    if (uiState.isAddContributionDialogOpen && uiState.contributingGoal != null) {
        AddGoalContributionDialog(
            goal = uiState.contributingGoal!!,
            onDismiss = onDismissAddContributionDialog,
            onConfirm = { amount, date, note ->
                onSaveGoalContribution(uiState.contributingGoal!!.id, amount, date, note)
            }
        )
    }

    // Archive Confirmation Dialog
    if (goalToDeleteConfirmation != null) {
        val targetGoal = goalToDeleteConfirmation!!
        AlertDialog(
            onDismissRequest = { goalToDeleteConfirmation = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Move Goal to Archive?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Goal \"${targetGoal.name}\" and its savings contributions will be safely moved to Archive. You can restore it anytime within 2 calendar months."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToArchive = targetGoal.id
                        goalToDeleteConfirmation = null
                        onArchiveGoal(idToArchive)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Archive Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoalsSummaryAnalyticsCard(
    totalTarget: Double,
    totalSaved: Double,
    overallProgressPercent: Double,
    thisMonthSaved: Double,
    totalMonthlyRequired: Double,
    savingCapacity: Double,
    isDeficit: Boolean,
    capacityDiff: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goals_summary_analytics_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Overall Savings Progress
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
                            text = "Target Savings Pool",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dedicated goal accumulation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IncomeGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${String.format("%.1f", overallProgressPercent)}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomeGreen
                    )
                }
            }

            // Progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = (overallProgressPercent / 100.0).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "goalsProgressAnim"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = IncomeGreen,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Metrics row: Total Saved vs Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Accumulated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatBDT(totalSaved),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomeGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Active Target",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatBDT(totalTarget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Monthly Saving Capacity & Requirements
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Goal Required",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatBDT(totalMonthlyRequired) + "/mo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Saving Capacity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatBDT(savingCapacity) + "/mo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDeficit) ExpenseRed else IncomeGreen
                    )
                }

                // Capacity status badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDeficit) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isDeficit) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (isDeficit) ExpenseRed else IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isDeficit) {
                                "Capacity Shortfall: ৳${CurrencyUtils.formatBDT(Math.abs(capacityDiff))} deficit compared to required targets."
                            } else {
                                "Comfortable Capacity: Surplus of ৳${CurrencyUtils.formatBDT(capacityDiff)} above goal targets."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDeficit) ExpenseRed else IncomeGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalFilterChipsBar(
    selectedStatus: GoalStatus?,
    onStatusSelect: (GoalStatus?) -> Unit,
    allCount: Int,
    activeCount: Int,
    completedCount: Int,
    pausedCount: Int,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelect(null) },
                label = { Text("All ($allCount)") },
                modifier = Modifier.testTag("goal_filter_chip_all"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        item {
            FilterChip(
                selected = selectedStatus == GoalStatus.ACTIVE,
                onClick = { onStatusSelect(if (selectedStatus == GoalStatus.ACTIVE) null else GoalStatus.ACTIVE) },
                label = { Text("Active ($activeCount)") },
                modifier = Modifier.testTag("goal_filter_chip_active"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        item {
            FilterChip(
                selected = selectedStatus == GoalStatus.COMPLETED,
                onClick = { onStatusSelect(if (selectedStatus == GoalStatus.COMPLETED) null else GoalStatus.COMPLETED) },
                label = { Text("Completed ($completedCount)") },
                modifier = Modifier.testTag("goal_filter_chip_completed"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        item {
            FilterChip(
                selected = selectedStatus == GoalStatus.PAUSED,
                onClick = { onStatusSelect(if (selectedStatus == GoalStatus.PAUSED) null else GoalStatus.PAUSED) },
                label = { Text("Paused ($pausedCount)") },
                modifier = Modifier.testTag("goal_filter_chip_paused"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun GoalItemCard(
    goalModel: FinancialGoalUiModel,
    isHistoryExpanded: Boolean,
    onToggleHistory: () -> Unit,
    onAddContribution: () -> Unit,
    onEditGoal: () -> Unit,
    onUpdateStatus: (GoalStatus) -> Unit,
    onArchiveGoal: () -> Unit,
    onDeleteContribution: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = goalModel.goal
    var isMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}")
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
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
            // Header row: Icon, Category, Goal Name, Badges, and Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(goal.category.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = goal.category.icon,
                        contentDescription = goal.category.displayName,
                        tint = goal.category.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = goal.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outline)
                        // Priority Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = goal.priority.color.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = goal.priority.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = goal.priority.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        // Status Badge
                        val statusBg = when (goal.status) {
                            GoalStatus.ACTIVE -> IncomeGreen.copy(alpha = 0.12f)
                            GoalStatus.COMPLETED -> LoanBlue.copy(alpha = 0.15f)
                            GoalStatus.PAUSED -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            GoalStatus.ARCHIVED -> Color.Gray.copy(alpha = 0.15f)
                        }
                        val statusColor = when (goal.status) {
                            GoalStatus.ACTIVE -> IncomeGreen
                            GoalStatus.COMPLETED -> LoanBlue
                            GoalStatus.PAUSED -> Color(0xFFD97706)
                            GoalStatus.ARCHIVED -> Color.Gray
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = goal.status.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier.testTag("goal_menu_btn_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Goal Options"
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Goal") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                isMenuOpen = false
                                onEditGoal()
                            }
                        )
                        if (goal.status == GoalStatus.ACTIVE) {
                            DropdownMenuItem(
                                text = { Text("Mark Completed") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen) },
                                onClick = {
                                    isMenuOpen = false
                                    onUpdateStatus(GoalStatus.COMPLETED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pause Goal") },
                                leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null) },
                                onClick = {
                                    isMenuOpen = false
                                    onUpdateStatus(GoalStatus.PAUSED)
                                }
                            )
                        } else if (goal.status == GoalStatus.PAUSED) {
                            DropdownMenuItem(
                                text = { Text("Resume Goal") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = IncomeGreen) },
                                onClick = {
                                    isMenuOpen = false
                                    onUpdateStatus(GoalStatus.ACTIVE)
                                }
                            )
                        } else if (goal.status == GoalStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text("Reopen as Active") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                onClick = {
                                    isMenuOpen = false
                                    onUpdateStatus(GoalStatus.ACTIVE)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Archive Goal", color = ExpenseRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) },
                            onClick = {
                                isMenuOpen = false
                                onArchiveGoal()
                            }
                        )
                    }
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { goalModel.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (goalModel.isCompleted) IncomeGreen else goal.category.color,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Amounts Row: Saved / Target & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = CurrencyUtils.formatBDT(goalModel.totalSaved),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomeGreen
                    )
                    Text(
                        text = "/ " + CurrencyUtils.formatBDT(goal.targetAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${String.format("%.1f", goalModel.progressPercent)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (goalModel.isCompleted) IncomeGreen else MaterialTheme.colorScheme.primary
                )
            }

            // Target Date & Monthly Savings Requirement Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Remaining Amount
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.formatBDT(goalModel.remainingAmount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (goalModel.remainingAmount > 0) MaterialTheme.colorScheme.onSurface else IncomeGreen
                        )
                    }
                }

                // Deadline or Required Monthly
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (goal.targetDate != null) "Monthly Need" else "Target Monthly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val monthlyText = if (goalModel.requiredMonthlySaving != null) {
                            CurrencyUtils.formatBDT(goalModel.requiredMonthlySaving) + "/mo"
                        } else if (goal.targetMonthlyContribution != null) {
                            CurrencyUtils.formatBDT(goal.targetMonthlyContribution) + "/mo"
                        } else {
                            "Flexible"
                        }
                        Text(
                            text = monthlyText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Target Date Badge if present
            if (goal.targetDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    val dateFormatted = DateUtils.formatShortDate(goal.targetDate)
                    val timeRemainingText = if (goalModel.isOverdue) {
                        "Target was $dateFormatted (Overdue)"
                    } else if (goalModel.remainingMonths != null) {
                        "Target: $dateFormatted (${goalModel.remainingMonths} mos left)"
                    } else {
                        "Target: $dateFormatted"
                    }
                    Text(
                        text = timeRemainingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (goalModel.isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Actions: Add Savings + History toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleHistory,
                    modifier = Modifier.testTag("toggle_history_${goal.id}")
                ) {
                    Icon(
                        imageVector = if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "History (${goalModel.contributions.size})",
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onAddContribution,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    modifier = Modifier.testTag("add_contribution_btn_${goal.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Savings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Expandable Contribution History
            AnimatedVisibility(
                visible = isHistoryExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contribution History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (goal.initialSavedAmount > 0) {
                            Text(
                                text = "Initial: ${CurrencyUtils.formatBDT(goal.initialSavedAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (goalModel.contributions.isEmpty()) {
                        Text(
                            text = "No contributions recorded yet. Tap 'Add Savings' to log deposits toward this goal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        goalModel.contributions.forEach { contribution ->
                            ContributionHistoryRow(
                                contribution = contribution,
                                onDelete = { onDeleteContribution(contribution.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributionHistoryRow(
    contribution: GoalContributionEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contribution_row_${contribution.id}"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CurrencyUtils.formatBDT(contribution.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = DateUtils.formatShortDate(contribution.contributionDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (contribution.note.isNotBlank()) {
                        Text(
                            text = "• ${contribution.note}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Contribution",
                    tint = ExpenseRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalsEmptyState(
    filterStatus: GoalStatus?,
    onCreateGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 16.dp)
            .testTag("goals_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(IncomeGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = if (filterStatus != null) "No ${filterStatus.displayName} Goals" else "No Financial Goals Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (filterStatus != null) {
                    "No goals found matching the \"${filterStatus.displayName}\" status filter."
                } else {
                    "Set dedicated savings targets for a new vehicle, computer, home, wedding, or emergency fund to track your progress and required monthly contributions."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = onCreateGoal,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("empty_state_create_goal_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Goal", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEditGoalDialog(
    editingGoal: FinancialGoalEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: GoalCategory,
        targetAmount: Double,
        initialSavedAmount: Double,
        targetDate: Long?,
        priority: GoalPriority,
        targetMonthlyContribution: Double?
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editingGoal?.name ?: "") }
    var category by remember { mutableStateOf(editingGoal?.category ?: GoalCategory.OTHER) }
    var targetAmountStr by remember { mutableStateOf(editingGoal?.targetAmount?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var initialSavedStr by remember { mutableStateOf(editingGoal?.initialSavedAmount?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var targetDate by remember { mutableStateOf<Long?>(editingGoal?.targetDate) }
    var priority by remember { mutableStateOf(editingGoal?.priority ?: GoalPriority.MEDIUM) }
    var customMonthlyStr by remember { mutableStateOf(editingGoal?.targetMonthlyContribution?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingGoal == null) "New Financial Goal" else "Edit Goal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_goal_dialog"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Goal Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError && it.isNotBlank()) nameError = false
                        },
                        label = { Text("Goal Name *") },
                        placeholder = { Text("e.g., New Laptop, Emergency Fund") },
                        isError = nameError,
                        supportingText = if (nameError) { { Text("Goal name cannot be empty") } } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_name_input")
                    )
                }

                // Category Selection Chips
                item {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(GoalCategory.values()) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                leadingIcon = {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (category == cat) MaterialTheme.colorScheme.onPrimaryContainer else cat.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(cat.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Target Amount
                item {
                    OutlinedTextField(
                        value = targetAmountStr,
                        onValueChange = {
                            targetAmountStr = it
                            if (amountError) amountError = false
                        },
                        label = { Text("Target Amount (৳) *") },
                        placeholder = { Text("e.g., 50000") },
                        prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError,
                        supportingText = if (amountError) { { Text("Please enter a valid amount > 0") } } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_target_amount_input")
                    )
                }

                // Initial Saved Amount (only on create or edit)
                item {
                    OutlinedTextField(
                        value = initialSavedStr,
                        onValueChange = { initialSavedStr = it },
                        label = { Text("Already Saved (৳)") },
                        placeholder = { Text("0") },
                        prefix = { Text("৳ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Amount already set aside before creating this goal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Priority Selection
                item {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoalPriority.values().forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio.displayName) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = prio.color.copy(alpha = 0.2f),
                                    selectedLabelColor = prio.color
                                )
                            )
                        }
                    }
                }

                // Target Date / Deadline
                item {
                    Text(
                        text = "Target Date (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (targetDate != null) cal.timeInMillis = targetDate!!
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                        }
                                        targetDate = selectedCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = targetDate?.let { DateUtils.formatShortDate(it) } ?: "Set Target Date",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (targetDate != null) {
                            TextButton(onClick = { targetDate = null }) {
                                Text("Clear")
                            }
                        }
                    }
                }

                // Optional Custom Target Monthly Contribution
                item {
                    OutlinedTextField(
                        value = customMonthlyStr,
                        onValueChange = { customMonthlyStr = it },
                        label = { Text("Planned Monthly Saving (৳/mo)") },
                        placeholder = { Text("e.g., 5000") },
                        prefix = { Text("৳ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Optional: Custom target monthly saving allocation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val targetAmount = targetAmountStr.toDoubleOrNull() ?: 0.0
                    val initialSaved = initialSavedStr.toDoubleOrNull() ?: 0.0
                    val customMonthly = customMonthlyStr.toDoubleOrNull()

                    var hasError = false
                    if (trimmedName.isBlank()) {
                        nameError = true
                        hasError = true
                    }
                    if (targetAmount <= 0.0) {
                        amountError = true
                        hasError = true
                    }

                    if (!hasError) {
                        onSave(
                            trimmedName,
                            category,
                            targetAmount,
                            initialSaved,
                            targetDate,
                            priority,
                            customMonthly
                        )
                    }
                },
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text(if (editingGoal == null) "Create Goal" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalContributionDialog(
    goal: FinancialGoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: Long, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Savings to ${goal.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_contribution_dialog"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Goal Target: ${CurrencyUtils.formatBDT(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        if (amountError) amountError = false
                    },
                    label = { Text("Savings Amount (৳) *") },
                    placeholder = { Text("e.g., 2000") },
                    prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError,
                    supportingText = if (amountError) { { Text("Please enter a valid amount > 0") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contribution_amount_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Source (Optional)") },
                    placeholder = { Text("e.g., Monthly bonus, Freelance payout") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IncomeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Note: Goal contributions are counted as dedicated savings accumulation, not ordinary expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) {
                        amountError = true
                    } else {
                        onConfirm(amount, date, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                modifier = Modifier.testTag("save_contribution_button")
            ) {
                Text("Confirm Deposit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
