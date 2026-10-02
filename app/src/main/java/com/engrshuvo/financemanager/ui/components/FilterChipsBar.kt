package com.engrshuvo.financemanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.ui.state.DateFilterOption
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.theme.LoanBlue

@Composable
fun FilterChipsBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedType: TransactionTypeFilter,
    onTypeSelect: (TransactionTypeFilter) -> Unit,
    selectedCategoryId: String?,
    onCategorySelect: (String?) -> Unit,
    selectedDateOption: DateFilterOption,
    onDateOptionSelect: (DateFilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("filter_chips_bar"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) onSearchQueryChange("")
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = "Toggle Search",
                        tint = if (isSearchExpanded || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchExpanded || searchQuery.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by note, category or amount…", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransactionTypeFilter.values().forEach { type ->
                val isSelected = selectedType == type
                val chipColor = when (type) {
                    TransactionTypeFilter.ALL -> MaterialTheme.colorScheme.primary
                    TransactionTypeFilter.INCOME -> IncomeGreen
                    TransactionTypeFilter.EXPENSE -> ExpenseRed
                    TransactionTypeFilter.LOAN -> LoanBlue
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelect(type) },
                    label = {
                        Text(
                            text = when (type) {
                                TransactionTypeFilter.ALL -> "All Types"
                                TransactionTypeFilter.INCOME -> "Income only"
                                TransactionTypeFilter.EXPENSE -> "Expense only"
                                TransactionTypeFilter.LOAN -> "Loans only"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.15f),
                        selectedLabelColor = chipColor
                    )
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            DateFilterOption.values().forEach { dateOption ->
                val isSelected = selectedDateOption == dateOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onDateOptionSelect(dateOption) },
                    label = {
                        Text(
                            text = dateOption.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Categories", fontSize = 11.sp) },
                shape = RoundedCornerShape(10.dp)
            )

            val categoriesToShow = when (selectedType) {
                TransactionTypeFilter.ALL, TransactionTypeFilter.LOAN -> CategoryCatalog.getAllCategories()
                TransactionTypeFilter.INCOME -> CategoryCatalog.incomeCategories
                TransactionTypeFilter.EXPENSE -> CategoryCatalog.expenseCategories
            }

            categoriesToShow.forEach { cat ->
                val isSelected = selectedCategoryId == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) onCategorySelect(null) else onCategorySelect(cat.id)
                    },
                    label = {
                        Text(
                            text = cat.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cat.color.copy(alpha = 0.15f),
                        selectedLabelColor = cat.color
                    )
                )
            }
        }
    }
}
