package com.engrshuvo.financemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.util.CurrencyUtils

@Composable
fun BudgetPlanningDialog(
    monthName: String,
    totalMonthlyIncome: Double,
    currentAllocations: List<CategoryAllocationUiModel>,
    isDraft: Boolean = false,
    onDismiss: () -> Unit,
    onSaveAllocations: (Map<String, Double>) -> Unit,
    onCopyFromPreviousMonth: () -> Unit
) {
    val allocationInputs = remember(currentAllocations) {
        mutableStateMapOf<String, String>().apply {
            currentAllocations.forEach { model ->
                if (model.allocatedAmount > 0) {
                    put(model.category.id, if (model.allocatedAmount % 1.0 == 0.0) model.allocatedAmount.toLong().toString() else model.allocatedAmount.toString())
                }
            }
        }
    }

    val liveTotalAllocated = allocationInputs.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val liveShortfall = (liveTotalAllocated - totalMonthlyIncome).coerceAtLeast(0.0)
    val liveUnallocated = (totalMonthlyIncome - liveTotalAllocated).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Plan Monthly Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isDraft) "For $monthName • Pre-filled draft from last month (unsaved)" else "For $monthName • Allocations do not deduct cash directly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                // Header summary stats
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly Income:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = CurrencyUtils.formatBDT(totalMonthlyIncome),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Allocated:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = CurrencyUtils.formatBDT(liveTotalAllocated),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (liveShortfall > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Shortfall",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Planned Shortfall:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "-${CurrencyUtils.formatBDT(liveShortfall)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Unallocated Income:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyUtils.formatBDT(liveUnallocated),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Copy from previous month option
                OutlinedButton(
                    onClick = onCopyFromPreviousMonth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_prev_month_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy Last Month's Budget Plans",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category allocation list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = currentAllocations,
                        key = { it.category.id }
                    ) { item ->
                        val cat = item.category
                        val currentText = allocationInputs[cat.id] ?: ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(cat.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    tint = cat.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (cat.id == "savings" || cat.id == "emergency") {
                                    Text(
                                        text = "Planned reserve • not an expense",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IncomeGreen,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = currentText,
                                onValueChange = { newVal ->
                                    val filtered = newVal.filter { it.isDigit() || it == '.' }
                                    allocationInputs[cat.id] = filtered
                                },
                                modifier = Modifier
                                    .width(115.dp)
                                    .testTag("budget_input_${cat.id}"),
                                placeholder = { Text("0", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                prefix = { Text("৳", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val map = allocationInputs.mapValues { (_, value) ->
                        value.toDoubleOrNull() ?: 0.0
                    }
                    onSaveAllocations(map)
                },
                modifier = Modifier.testTag("save_budget_allocations_button")
            ) {
                Text("Save Allocations")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
