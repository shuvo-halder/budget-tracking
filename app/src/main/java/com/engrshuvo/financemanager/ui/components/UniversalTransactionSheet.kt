package com.engrshuvo.financemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionCategory
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.theme.LoanBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UniversalTransactionSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    editingTransaction: TransactionEntity?,
    defaultEntryType: TransactionType,
    defaultLoanType: LoanType,
    onSaveTransaction: (id: Long, type: TransactionType, amount: Double, categoryId: String, categoryName: String, note: String, timestamp: Long) -> Unit,
    onSaveLoan: (type: LoanType, personName: String, phone: String, amount: Double, startDate: Long, dueDate: Long?, note: String) -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedEntryType by remember(editingTransaction, defaultEntryType) {
        mutableStateOf(editingTransaction?.type ?: defaultEntryType)
    }

    var selectedLoanType by remember(defaultLoanType) {
        mutableStateOf(defaultLoanType)
    }

    var amountText by remember(editingTransaction) {
        mutableStateOf(
            if (editingTransaction != null) {
                if (editingTransaction.amount % 1.0 == 0.0) {
                    editingTransaction.amount.toLong().toString()
                } else {
                    editingTransaction.amount.toString()
                }
            } else ""
        )
    }

    var personNameText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }
    var noteText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }

    var selectedTimestamp by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis())
    }

    var dueDateTimestamp by remember { mutableStateOf<Long?>(null) }

    var selectedCategory by remember(editingTransaction, selectedEntryType) {
        mutableStateOf<TransactionCategory?>(
            if (editingTransaction != null) {
                CategoryCatalog.getCategoryById(editingTransaction.categoryId)
            } else {
                if (selectedEntryType == TransactionType.EXPENSE) {
                    CategoryCatalog.expenseCategories.firstOrNull()
                } else {
                    CategoryCatalog.incomeCategories.firstOrNull()
                }
            }
        )
    }

    var isSubmitting by remember(editingTransaction, isOpen) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSelectingDueDate by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val initialMillis = if (isSelectingDueDate) (dueDateTimestamp ?: System.currentTimeMillis()) else selectedTimestamp
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        if (isSelectingDueDate) {
                            dueDateTimestamp = it
                        } else {
                            selectedTimestamp = it
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val themeColor = when (selectedEntryType) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.LOAN -> if (selectedLoanType == LoanType.LENT) IncomeGreen else LoanBlue
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = Modifier.testTag("universal_transaction_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 20.dp, bottom = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) "Edit Entry" else "New Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close sheet")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = when (selectedEntryType) {
                    TransactionType.EXPENSE -> 0
                    TransactionType.INCOME -> 1
                    TransactionType.LOAN -> 2
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedEntryType == TransactionType.EXPENSE,
                    onClick = {
                        selectedEntryType = TransactionType.EXPENSE
                        if (selectedCategory == null || selectedCategory?.type != TransactionType.EXPENSE) {
                            selectedCategory = CategoryCatalog.expenseCategories.firstOrNull()
                        }
                    },
                    text = {
                        Text(
                            text = "Expense",
                            fontWeight = if (selectedEntryType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedEntryType == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedEntryType == TransactionType.INCOME,
                    onClick = {
                        selectedEntryType = TransactionType.INCOME
                        if (selectedCategory == null || selectedCategory?.type != TransactionType.INCOME) {
                            selectedCategory = CategoryCatalog.incomeCategories.firstOrNull()
                        }
                    },
                    text = {
                        Text(
                            text = "Income",
                            fontWeight = if (selectedEntryType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedEntryType == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedEntryType == TransactionType.LOAN,
                    onClick = {
                        selectedEntryType = TransactionType.LOAN
                    },
                    text = {
                        Text(
                            text = "Loan / Debt",
                            fontWeight = if (selectedEntryType == TransactionType.LOAN) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedEntryType == TransactionType.LOAN) LoanBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Amount (BDT ৳)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                        errorMessage = null
                    }
                },
                placeholder = { Text("0.00", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Text(
                        text = "৳",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor,
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("universal_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickAmounts = listOf(100, 500, 1000, 2000, 5000)
                quickAmounts.forEach { quickVal ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clickable {
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                val updated = current + quickVal
                                amountText = if (updated % 1.0 == 0.0) updated.toLong().toString() else updated.toString()
                                errorMessage = null
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "+৳$quickVal",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedEntryType == TransactionType.LOAN) {
                Text(
                    text = "Loan Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedLoanType = LoanType.LENT },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedLoanType == LoanType.LENT) IncomeGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (selectedLoanType == LoanType.LENT) androidx.compose.foundation.BorderStroke(2.dp, IncomeGreen) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Lent to someone",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedLoanType == LoanType.LENT) IncomeGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "You will receive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedLoanType = LoanType.BORROWED },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedLoanType == LoanType.BORROWED) LoanBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (selectedLoanType == LoanType.BORROWED) androidx.compose.foundation.BorderStroke(2.dp, LoanBlue) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Borrowed",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedLoanType == LoanType.BORROWED) LoanBlue else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "You owe them",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Person's Name *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = personNameText,
                    onValueChange = { personNameText = it; errorMessage = null },
                    placeholder = { Text("e.g. Rahul, Tanvir, Boss, Sister…") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = themeColor)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_person_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Contact Phone (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    placeholder = { Text("+880 1700 000000") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable {
                            isSelectingDueDate = true
                            showDatePicker = true
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Due Date (Optional Repayment Target)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (dueDateTimestamp != null) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueDateTimestamp!!)) else "No due date set",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = if (dueDateTimestamp != null) "Change" else "Set Due Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val availableCategories = if (selectedEntryType == TransactionType.EXPENSE) {
                    CategoryCatalog.expenseCategories
                } else {
                    CategoryCatalog.incomeCategories
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { category ->
                        val isSelected = selectedCategory?.id == category.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) category.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, category.color) else null,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clickable {
                                    selectedCategory = category
                                    errorMessage = null
                                }
                                .testTag("cat_chip_${category.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) category.color else category.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else category.color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) category.color else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Note / Remarks",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Add any extra details or reference…") },
                leadingIcon = {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable {
                        isSelectingDueDate = false
                        showDatePicker = true
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date(selectedTimestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpenseRed,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }

                    if (selectedEntryType == TransactionType.LOAN) {
                        if (personNameText.isBlank()) {
                            errorMessage = "Please enter the person's name"
                            return@Button
                        }
                        isSubmitting = true
                        onSaveLoan(
                            selectedLoanType,
                            personNameText,
                            phoneText,
                            amount,
                            selectedTimestamp,
                            dueDateTimestamp,
                            noteText
                        )
                    } else {
                        val cat = selectedCategory
                        if (cat == null) {
                            errorMessage = "Please select a category"
                            return@Button
                        }
                        isSubmitting = true
                        onSaveTransaction(
                            editingTransaction?.id ?: 0L,
                            selectedEntryType,
                            amount,
                            cat.id,
                            cat.name,
                            noteText,
                            selectedTimestamp
                        )
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("universal_save_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingTransaction != null) "Update Entry" else "Save ${when (selectedEntryType) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.LOAN -> "Loan"
                    }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
