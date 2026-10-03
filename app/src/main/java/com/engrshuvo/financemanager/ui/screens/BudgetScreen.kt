package com.engrshuvo.financemanager.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.util.CurrencyUtils

@Composable
fun BudgetScreen(
    monthName: String,
    totalMonthlyIncome: Double,
    totalAllocated: Double,
    unallocatedIncome: Double,
    plannedShortfall: Double,
    isShortfall: Boolean,
    plannedSavingsTotal: Double,
    dailyLimit: Double,
    todayExpenses: Double,
    dailyRemaining: Double,
    dailyProgress: Float,
    isDailyOverBudget: Boolean,
    allocations: List<CategoryAllocationUiModel>,
    isDraft: Boolean = false,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPlanBudgetClick: () -> Unit,
    onConfigureDailyLimitClick: () -> Unit,
    onCopyFromPreviousMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Switcher Header
        item(key = "budget_month_switcher") {
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
                    IconButton(onClick = onPreviousMonth) {
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

                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month"
                        )
                    }
                }
            }
        }

        // Planned Shortfall Alert Banner
        if (isShortfall) {
            item(key = "budget_shortfall_card") {
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
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDraft) "Draft Budget Shortfall Notice" else "Budget Shortfall Alert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Text(
                                text = if (isDraft) {
                                    "Draft targets copied from last month (${CurrencyUtils.formatBDT(totalAllocated)}) exceed current income (${CurrencyUtils.formatBDT(totalMonthlyIncome)}) by ${CurrencyUtils.formatBDT(plannedShortfall)}. These are unsaved draft targets; adjust or confirm them in Plan / Edit."
                                } else {
                                    "Allocations (${CurrencyUtils.formatBDT(totalAllocated)}) exceed monthly income (${CurrencyUtils.formatBDT(totalMonthlyIncome)}) by ${CurrencyUtils.formatBDT(plannedShortfall)}."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )
                        }

                        TextButton(onClick = onPlanBudgetClick) {
                            Text("Adjust", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Budget Planning Overview Card
        item(key = "budget_overview_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Income vs Plans",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Allocations plan your salary; actual expenses reduce cash.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onPlanBudgetClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("plan_budget_screen_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Plan / Edit", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                Text("Monthly Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyUtils.formatBDT(totalMonthlyIncome), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Total Allocated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyUtils.formatBDT(totalAllocated), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isShortfall) ExpenseRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(if (isShortfall) "Shortfall" else "Unallocated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (isShortfall) "-${CurrencyUtils.formatBDT(plannedShortfall)}" else CurrencyUtils.formatBDT(unallocatedIncome),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isShortfall) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (plannedSavingsTotal > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Planned Savings Reserve: ${CurrencyUtils.formatBDT(plannedSavingsTotal)} (earmarked, not an expense)",
                                style = MaterialTheme.typography.bodySmall,
                                color = IncomeGreen,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Daily Limit Configuration Card
        item(key = "budget_daily_limit_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Daily Living Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (isDailyOverBudget) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = ExpenseRed.copy(alpha = 0.15f)) {
                                        Text("Over Limit", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(
                                text = if (dailyLimit > 0) "${CurrencyUtils.formatBDT(todayExpenses)} of ${CurrencyUtils.formatBDT(dailyLimit)} spent today" else "No daily ceiling set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onConfigureDailyLimitClick) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = "Configure Daily Limit", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (dailyLimit > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { dailyProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isDailyOverBudget) ExpenseRed else if (dailyProgress >= 0.8f) Color(0xFFF59E0B) else IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isDailyOverBudget) "Overspent Today:" else "Remaining Today:", style = MaterialTheme.typography.bodySmall, color = if (isDailyOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.formatBDT(Math.abs(dailyRemaining)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isDailyOverBudget) ExpenseRed else IncomeGreen)
                        }
                    }
                }
            }
        }

        // Section Title: Category Allocations
        item(key = "budget_allocations_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Category Allocations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isDraft) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Draft (Unsaved)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onCopyFromPreviousMonth,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Last Month", fontSize = 11.sp)
                }
            }
        }

        // Category Allocation Cards
        items(
            items = allocations,
            key = { "alloc_${it.category.id}" }
        ) { model ->
            val cat = model.category
            val isOver = model.isOverBudget
            val isAllocated = model.allocatedAmount > 0
            val progressClamped = model.usagePercentage.coerceIn(0f, 1f)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cat.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (isOver) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = ExpenseRed.copy(alpha = 0.15f)) {
                                        Text("Over Budget", color = ExpenseRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text(
                                text = if (isAllocated) "${CurrencyUtils.formatBDT(model.actualSpent)} spent of ${CurrencyUtils.formatBDT(model.allocatedAmount)}" else "${CurrencyUtils.formatBDT(model.actualSpent)} spent (unallocated)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isAllocated) {
                                    if (isOver) "-${CurrencyUtils.formatBDT(Math.abs(model.remainingAmount))}" else CurrencyUtils.formatBDT(model.remainingAmount)
                                } else "৳0",
                                style = MaterialTheme.typography.titleSmall,
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
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progressClamped },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isOver) ExpenseRed else cat.color,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
