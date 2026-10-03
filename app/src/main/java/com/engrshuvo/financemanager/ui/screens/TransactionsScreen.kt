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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.ui.components.FilterChipsBar
import com.engrshuvo.financemanager.ui.components.TransactionItemCard
import com.engrshuvo.financemanager.ui.state.DateFilterOption
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.util.CurrencyUtils

@Composable
fun TransactionsScreen(
    filteredTransactions: List<TransactionEntity>,
    allTransactionsCount: Int,
    searchQuery: String,
    selectedType: TransactionTypeFilter,
    selectedCategoryId: String?,
    selectedDateOption: DateFilterOption,
    onSearchQueryChange: (String) -> Unit,
    onTypeSelect: (TransactionTypeFilter) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onDateOptionSelect: (DateFilterOption) -> Unit,
    onClearFilters: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Action Buttons
        item(key = "tx_quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddIncomeClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_add_income_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Income / Salary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_add_expense_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(14.dp),
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

        // Filter and Search Bar
        item(key = "tx_filter_bar") {
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

        // Summary row
        item(key = "tx_summary_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Records (${filteredTransactions.size} of $allTransactionsCount)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                if (searchQuery.isNotBlank() || selectedType != TransactionTypeFilter.ALL || selectedCategoryId != null || selectedDateOption != DateFilterOption.ALL_TIME) {
                    Text(
                        text = "Filtered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item(key = "tx_empty_state") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
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
                                imageVector = if (allTransactionsCount > 0) Icons.Default.FilterAltOff else Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = if (allTransactionsCount > 0) "No matching records" else "No transactions recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (allTransactionsCount > 0) "Try clearing your filters or search keywords." else "Record your first income or expense entry using the buttons above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (allTransactionsCount > 0) {
                            OutlinedButton(
                                onClick = onClearFilters,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredTransactions,
                key = { "tx_${it.id}" }
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
