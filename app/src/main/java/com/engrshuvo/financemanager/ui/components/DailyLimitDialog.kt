package com.engrshuvo.financemanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.ui.util.CurrencyUtils

@Composable
fun DailyLimitDialog(
    currentLimit: Double,
    suggestedDailyLimit: Double,
    onDismiss: () -> Unit,
    onSaveLimit: (Double) -> Unit
) {
    var limitInput by remember(currentLimit) {
        val formatted = if (currentLimit % 1.0 == 0.0) currentLimit.toLong().toString() else currentLimit.toString()
        mutableStateOf(formatted)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily Spending Limit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Set a daily expense ceiling to monitor today's living expenses independently from monthly allocations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { newVal ->
                        limitInput = newVal.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("Daily Limit Amount") },
                    prefix = { Text("৳ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_limit_input")
                )

                if (suggestedDailyLimit > 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Monthly Daily Expenses Suggestion:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Calculated from your monthly Daily Expenses allocation: ${CurrencyUtils.formatBDT(suggestedDailyLimit)} / day.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    val rounded = Math.round(suggestedDailyLimit).toString()
                                    limitInput = rounded
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("use_suggested_daily_button")
                            ) {
                                Text("Use Suggested (${CurrencyUtils.formatBDT(suggestedDailyLimit)})")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = limitInput.toDoubleOrNull() ?: 0.0
                    onSaveLimit(amount)
                },
                modifier = Modifier.testTag("save_daily_limit_button")
            ) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
