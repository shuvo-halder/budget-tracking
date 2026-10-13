package com.engrshuvo.financemanager.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.GoalStatus
import com.engrshuvo.financemanager.data.model.PdfExportFilterState
import com.engrshuvo.financemanager.data.model.ReportCategory
import com.engrshuvo.financemanager.data.model.ReportType
import com.engrshuvo.financemanager.ui.state.FinanceUiState
import com.engrshuvo.financemanager.ui.state.LoanTypeFilter
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.theme.ExpenseRed
import com.engrshuvo.financemanager.ui.theme.IncomeGreen
import com.engrshuvo.financemanager.ui.theme.LoanBlue
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportScreen(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onUpdateFilterState: ((PdfExportFilterState) -> PdfExportFilterState) -> Unit,
    onResetFilterState: () -> Unit,
    onGeneratePdfToUri: (android.net.Uri, android.content.Context) -> Unit,
    onSharePdfReport: (android.content.Context) -> Unit,
    onOpenPdfReport: (android.content.Context, android.net.Uri) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val filterState = uiState.pdfFilterState

    // SAF Document Creator Launcher
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            onGeneratePdfToUri(uri, context)
        }
    }

    var activeCategoryTab by remember(filterState.selectedReportType) {
        mutableStateOf(filterState.selectedReportType.category)
    }

    // Date Validation
    val isDateRangeInvalid = when (filterState.selectedReportType) {
        ReportType.TRANSACTION_HISTORY,
        ReportType.LOAN_REPAYMENT_HISTORY,
        ReportType.CUSTOM_DATE_TRANSACTIONS -> filterState.startDateTimestamp > filterState.endDateTimestamp
        else -> false
    }

    // Dynamic Summary calculation
    val summaryInfo = remember(uiState, filterState) {
        calculateReportSummary(uiState, filterState)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdf_export_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "PDF Export Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("pdf_export_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onResetFilterState,
                        modifier = Modifier.testTag("pdf_export_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset filters"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success or Error Banner
            if (uiState.pdfExportMessage != null) {
                item(key = "pdf_message_banner") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = IncomeGreen.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = IncomeGreen
                                )
                                Text(
                                    text = uiState.pdfExportMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onDismissMessage) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            if (uiState.pdfExportError != null) {
                item(key = "pdf_error_banner") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ExpenseRed.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = ExpenseRed
                                )
                                Text(
                                    text = uiState.pdfExportError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onDismissMessage) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            // SECTION 1: REPORT TYPE SELECTION
            item(key = "section_report_type_header") {
                Column {
                    Text(
                        text = "1. Select Report Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Choose from 12 specialized financial statements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "report_category_tabs") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReportCategory.values(), key = { it.name }) { category ->
                        val isSelected = activeCategoryTab == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeCategoryTab = category },
                            label = { Text(category.displayName, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("report_category_${category.name.lowercase()}")
                        )
                    }
                }
            }

            val categoryReports = ReportType.values().filter { it.category == activeCategoryTab }
            items(categoryReports, key = { it.id }) { reportType ->
                val isSelected = filterState.selectedReportType == reportType
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onUpdateFilterState { it.copy(selectedReportType = reportType) }
                        }
                        .testTag("report_type_${reportType.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onUpdateFilterState { it.copy(selectedReportType = reportType) } }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reportType.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = reportType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // SECTION 2: PERIOD SELECTION CONTROLS
            item(key = "section_period_header") {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "2. Select Report Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Configure year, month, day, or custom start and end dates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "period_controls") {
                val currentReport = filterState.selectedReportType
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (currentReport) {
                            ReportType.MONTHLY_EXPENSE,
                            ReportType.MONTHLY_INCOME,
                            ReportType.CATEGORY_EXPENSE,
                            ReportType.MONTHLY_SUMMARY,
                            ReportType.BUDGET_VS_ACTUAL,
                            ReportType.SAVINGS_REPORT,
                            ReportType.LOAN_SUMMARY,
                            ReportType.FINANCIAL_GOALS -> {
                                MonthlyPeriodPicker(
                                    filterState = filterState,
                                    onUpdateFilterState = onUpdateFilterState
                                )
                            }

                            ReportType.DAILY_EXPENSE -> {
                                DailyPeriodPicker(
                                    context = context,
                                    filterState = filterState,
                                    onUpdateFilterState = onUpdateFilterState
                                )
                            }

                            ReportType.TRANSACTION_HISTORY,
                            ReportType.LOAN_REPAYMENT_HISTORY,
                            ReportType.CUSTOM_DATE_TRANSACTIONS -> {
                                DateRangePickerControls(
                                    context = context,
                                    filterState = filterState,
                                    onUpdateFilterState = onUpdateFilterState
                                )
                            }
                        }

                        if (isDateRangeInvalid) {
                            Text(
                                text = "⚠️ Start date cannot be after end date. Please adjust your range.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // SECTION 3: OPTIONAL FILTERS
            item(key = "section_filters_header") {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "3. Apply Optional Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Refine data by category, transaction type, or loan status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "optional_filters_content") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentReport = filterState.selectedReportType

                        // Category Filter (for expense/income reports)
                        if (currentReport in listOf(
                                ReportType.MONTHLY_EXPENSE,
                                ReportType.MONTHLY_INCOME,
                                ReportType.DAILY_EXPENSE,
                                ReportType.CATEGORY_EXPENSE,
                                ReportType.TRANSACTION_HISTORY,
                                ReportType.CUSTOM_DATE_TRANSACTIONS
                            )
                        ) {
                            CategoryFilterDropdown(
                                selectedCatId = filterState.selectedCategoryFilterId,
                                onSelectCategory = { catId ->
                                    onUpdateFilterState { it.copy(selectedCategoryFilterId = catId) }
                                }
                            )
                        }

                        // Transaction Type Filter
                        if (currentReport in listOf(
                                ReportType.TRANSACTION_HISTORY,
                                ReportType.CUSTOM_DATE_TRANSACTIONS
                            )
                        ) {
                            TransactionTypeFilterRow(
                                selectedType = filterState.selectedTransactionTypeFilter,
                                onSelectType = { type ->
                                    onUpdateFilterState { it.copy(selectedTransactionTypeFilter = type) }
                                }
                            )
                        }

                        // Loan Type Filter
                        if (currentReport in listOf(
                                ReportType.LOAN_SUMMARY,
                                ReportType.LOAN_REPAYMENT_HISTORY
                            )
                        ) {
                            LoanTypeFilterRow(
                                selectedType = filterState.selectedLoanTypeFilter,
                                onSelectType = { type ->
                                    onUpdateFilterState { it.copy(selectedLoanTypeFilter = type) }
                                }
                            )
                        }

                        // Goal Status Filter
                        if (currentReport == ReportType.FINANCIAL_GOALS) {
                            GoalStatusFilterRow(
                                selectedStatus = filterState.selectedGoalStatusFilter,
                                onSelectStatus = { status ->
                                    onUpdateFilterState { it.copy(selectedGoalStatusFilter = status) }
                                }
                            )
                        }
                    }
                }
            }

            // SECTION 4: EXPORT SUMMARY CARD
            item(key = "section_summary") {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Export Preview Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Report: ${summaryInfo.reportTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Period: ${summaryInfo.periodDescription}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Applied Filters: ${summaryInfo.filterDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Matching Records: ${summaryInfo.recordCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (summaryInfo.totalAmountText != null) {
                                Text(
                                    text = summaryInfo.totalAmountText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 5: ACTION BUTTONS
            item(key = "section_actions") {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            createDocLauncher.launch(filterState.getSuggestedFileName())
                        },
                        enabled = !uiState.isGeneratingPdf && !isDateRangeInvalid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_pdf_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating PDF...")
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate & Save PDF", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSharePdfReport(context) },
                            enabled = !uiState.isGeneratingPdf && !isDateRangeInvalid,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_pdf_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF")
                        }

                        if (uiState.pdfExportFileUri != null) {
                            Button(
                                onClick = { onOpenPdfReport(context, uiState.pdfExportFileUri) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("open_pdf_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IncomeGreen
                                )
                            ) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyPeriodPicker(
    filterState: PdfExportFilterState,
    onUpdateFilterState: ((PdfExportFilterState) -> PdfExportFilterState) -> Unit
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    var showMonthMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("month_year_picker"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    val prevMonth = if (filterState.selectedMonth == 1) 12 else filterState.selectedMonth - 1
                    val prevYear = if (filterState.selectedMonth == 1) filterState.selectedYear - 1 else filterState.selectedYear
                    onUpdateFilterState { it.copy(selectedMonth = prevMonth, selectedYear = prevYear) }
                }
            ) {
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Box {
                OutlinedButton(onClick = { showMonthMenu = true }) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${monthNames.getOrNull(filterState.selectedMonth - 1) ?: "Month"} ${filterState.selectedYear}")
                }
                DropdownMenu(
                    expanded = showMonthMenu,
                    onDismissRequest = { showMonthMenu = false }
                ) {
                    monthNames.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                showMonthMenu = false
                                onUpdateFilterState { it.copy(selectedMonth = index + 1) }
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    val nextMonth = if (filterState.selectedMonth == 12) 1 else filterState.selectedMonth + 1
                    val nextYear = if (filterState.selectedMonth == 12) filterState.selectedYear + 1 else filterState.selectedYear
                    onUpdateFilterState { it.copy(selectedMonth = nextMonth, selectedYear = nextYear) }
                }
            ) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }

        // Year selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onUpdateFilterState { it.copy(selectedYear = it.selectedYear - 1) } }
            ) {
                Text("-1y", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = filterState.selectedYear.toString(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(
                onClick = { onUpdateFilterState { it.copy(selectedYear = it.selectedYear + 1) } }
            ) {
                Text("+1y", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DailyPeriodPicker(
    context: android.content.Context,
    filterState: PdfExportFilterState,
    onUpdateFilterState: ((PdfExportFilterState) -> PdfExportFilterState) -> Unit
) {
    val cal = remember(filterState.selectedDailyTimestamp) {
        Calendar.getInstance().apply { timeInMillis = filterState.selectedDailyTimestamp }
    }
    val formattedDate = remember(filterState.selectedDailyTimestamp) {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US).format(Date(filterState.selectedDailyTimestamp))
    }

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val updated = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            onUpdateFilterState { it.copy(selectedDailyTimestamp = updated.timeInMillis) }
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = {
                val prev = Calendar.getInstance().apply {
                    timeInMillis = filterState.selectedDailyTimestamp
                    add(Calendar.DAY_OF_MONTH, -1)
                }
                onUpdateFilterState { it.copy(selectedDailyTimestamp = prev.timeInMillis) }
            }
        ) {
            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Day")
        }

        OutlinedButton(onClick = { datePicker.show() }) {
            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(formattedDate, fontWeight = FontWeight.Bold)
        }

        IconButton(
            onClick = {
                val next = Calendar.getInstance().apply {
                    timeInMillis = filterState.selectedDailyTimestamp
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                onUpdateFilterState { it.copy(selectedDailyTimestamp = next.timeInMillis) }
            }
        ) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Day")
        }
    }
}

@Composable
private fun DateRangePickerControls(
    context: android.content.Context,
    filterState: PdfExportFilterState,
    onUpdateFilterState: ((PdfExportFilterState) -> PdfExportFilterState) -> Unit
) {
    val startCal = Calendar.getInstance().apply { timeInMillis = filterState.startDateTimestamp }
    val endCal = Calendar.getInstance().apply { timeInMillis = filterState.endDateTimestamp }

    val startFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(startCal.time)
    val endFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(endCal.time)

    val startDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val updated = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            onUpdateFilterState { it.copy(startDateTimestamp = updated.timeInMillis) }
        },
        startCal.get(Calendar.YEAR),
        startCal.get(Calendar.MONTH),
        startCal.get(Calendar.DAY_OF_MONTH)
    )

    val endDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val updated = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            onUpdateFilterState { it.copy(endDateTimestamp = updated.timeInMillis) }
        },
        endCal.get(Calendar.YEAR),
        endCal.get(Calendar.MONTH),
        endCal.get(Calendar.DAY_OF_MONTH)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Start Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { startDatePicker.show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_date_button")
            ) {
                Text(startFormatted, fontSize = 12.sp, maxLines = 1)
            }
        }

        Text("to", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

        Column(modifier = Modifier.weight(1f)) {
            Text("End Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { endDatePicker.show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("end_date_button")
            ) {
                Text(endFormatted, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CategoryFilterDropdown(
    selectedCatId: String?,
    onSelectCategory: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = remember { CategoryCatalog.getAllCategories() }
    val selectedCategoryName = categories.find { it.id == selectedCatId }?.name ?: "All Categories"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Expense / Income Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_filter_dropdown")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedCategoryName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All Categories") },
                    onClick = {
                        expanded = false
                        onSelectCategory(null)
                    }
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            expanded = false
                            onSelectCategory(category.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionTypeFilterRow(
    selectedType: TransactionTypeFilter,
    onSelectType: (TransactionTypeFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Transaction Type", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TransactionTypeFilter.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onSelectType(type) },
                    label = { Text(type.name, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun LoanTypeFilterRow(
    selectedType: LoanTypeFilter,
    onSelectType: (LoanTypeFilter) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag("loan_type_filter_row")
    ) {
        Text("Loan Direction", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LoanTypeFilter.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onSelectType(type) },
                    label = { Text(type.name, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun GoalStatusFilterRow(
    selectedStatus: GoalStatus?,
    onSelectStatus: (GoalStatus?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Goal Status", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onSelectStatus(null) },
                label = { Text("ALL", fontSize = 12.sp) }
            )
            GoalStatus.values().forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onSelectStatus(status) },
                    label = { Text(status.name, fontSize = 12.sp) }
                )
            }
        }
    }
}

private data class DynamicReportSummary(
    val reportTitle: String,
    val periodDescription: String,
    val filterDescription: String,
    val recordCount: Int,
    val totalAmountText: String?
)

private fun calculateReportSummary(
    uiState: FinanceUiState,
    filterState: PdfExportFilterState
): DynamicReportSummary {
    val title = filterState.selectedReportType.title
    val monthStart = PdfExportFilterState.getStartOfMonthTimestamp(filterState.selectedYear, filterState.selectedMonth)
    val monthEnd = PdfExportFilterState.getEndOfMonthTimestamp(filterState.selectedYear, filterState.selectedMonth)

    val periodDesc = when (filterState.selectedReportType) {
        ReportType.MONTHLY_EXPENSE,
        ReportType.MONTHLY_INCOME,
        ReportType.CATEGORY_EXPENSE,
        ReportType.MONTHLY_SUMMARY,
        ReportType.BUDGET_VS_ACTUAL,
        ReportType.SAVINGS_REPORT,
        ReportType.LOAN_SUMMARY,
        ReportType.FINANCIAL_GOALS -> filterState.getFormattedMonthYear()

        ReportType.DAILY_EXPENSE -> SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(filterState.selectedDailyTimestamp))

        ReportType.TRANSACTION_HISTORY,
        ReportType.LOAN_REPAYMENT_HISTORY,
        ReportType.CUSTOM_DATE_TRANSACTIONS -> {
            val start = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(filterState.startDateTimestamp))
            val end = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(filterState.endDateTimestamp))
            "$start to $end"
        }
    }

    var filterDesc = "All Records"
    var count = 0
    var totalText: String? = null

    when (filterState.selectedReportType) {
        ReportType.MONTHLY_EXPENSE -> {
            val txs = uiState.allTransactions.filter {
                it.type == com.engrshuvo.financemanager.data.model.TransactionType.EXPENSE &&
                        it.timestamp in monthStart..monthEnd &&
                        (filterState.selectedCategoryFilterId == null || it.categoryId == filterState.selectedCategoryFilterId)
            }
            count = txs.size
            val sum = txs.sumOf { it.amount }
            totalText = CurrencyUtils.formatBDT(sum)
            filterDesc = if (filterState.selectedCategoryFilterId != null)
                "Category: ${CategoryCatalog.getCategoryById(filterState.selectedCategoryFilterId).name}"
            else "All Expense Categories"
        }

        ReportType.MONTHLY_INCOME -> {
            val txs = uiState.allTransactions.filter {
                it.type == com.engrshuvo.financemanager.data.model.TransactionType.INCOME &&
                        it.timestamp in monthStart..monthEnd &&
                        (filterState.selectedCategoryFilterId == null || it.categoryId == filterState.selectedCategoryFilterId)
            }
            count = txs.size
            val sum = txs.sumOf { it.amount }
            totalText = CurrencyUtils.formatBDT(sum)
            filterDesc = "All Income Sources"
        }

        ReportType.DAILY_EXPENSE -> {
            val dayStart = com.engrshuvo.financemanager.ui.util.DateUtils.getStartOfDay(filterState.selectedDailyTimestamp)
            val dayEnd = com.engrshuvo.financemanager.ui.util.DateUtils.getEndOfDay(filterState.selectedDailyTimestamp)
            val txs = uiState.allTransactions.filter {
                it.type == com.engrshuvo.financemanager.data.model.TransactionType.EXPENSE &&
                        it.timestamp in dayStart..dayEnd
            }
            count = txs.size
            val sum = txs.sumOf { it.amount }
            totalText = CurrencyUtils.formatBDT(sum)
        }

        ReportType.CATEGORY_EXPENSE -> {
            val txs = uiState.allTransactions.filter {
                it.type == com.engrshuvo.financemanager.data.model.TransactionType.EXPENSE &&
                        it.timestamp in monthStart..monthEnd
            }
            count = txs.size
            val sum = txs.sumOf { it.amount }
            totalText = CurrencyUtils.formatBDT(sum)
        }

        ReportType.TRANSACTION_HISTORY,
        ReportType.CUSTOM_DATE_TRANSACTIONS -> {
            val txs = uiState.allTransactions.filter {
                it.timestamp in filterState.startDateTimestamp..filterState.endDateTimestamp &&
                        (filterState.selectedCategoryFilterId == null || it.categoryId == filterState.selectedCategoryFilterId) &&
                        when (filterState.selectedTransactionTypeFilter) {
                            TransactionTypeFilter.ALL -> true
                            TransactionTypeFilter.INCOME -> it.type == com.engrshuvo.financemanager.data.model.TransactionType.INCOME
                            TransactionTypeFilter.EXPENSE -> it.type == com.engrshuvo.financemanager.data.model.TransactionType.EXPENSE
                            TransactionTypeFilter.LOAN -> it.type == com.engrshuvo.financemanager.data.model.TransactionType.LOAN
                        }
            }
            count = txs.size
            val sum = txs.sumOf { if (it.type == com.engrshuvo.financemanager.data.model.TransactionType.EXPENSE) -it.amount else it.amount }
            totalText = "Net: ${CurrencyUtils.formatBDT(sum)}"
        }

        ReportType.MONTHLY_SUMMARY -> {
            val txs = uiState.allTransactions.filter { it.timestamp in monthStart..monthEnd }
            count = txs.size
            val income = txs.filter { it.type == com.engrshuvo.financemanager.data.model.TransactionType.INCOME }.sumOf { it.amount }
            val expense = txs.filter { it.type == com.engrshuvo.financemanager.data.model.TransactionType.EXPENSE }.sumOf { it.amount }
            totalText = "Net: ${CurrencyUtils.formatBDT(income - expense)}"
        }

        ReportType.BUDGET_VS_ACTUAL -> {
            count = uiState.monthAllocations.size
            val spent = uiState.monthAllocations.sumOf { it.actualSpent }
            totalText = "Spent: ${CurrencyUtils.formatBDT(spent)}"
        }

        ReportType.SAVINGS_REPORT -> {
            val savingsAlloc = uiState.monthAllocations.filter { it.category.id == "savings" || it.category.id == "emergency" }.sumOf { it.allocatedAmount }
            count = uiState.activeGoals.size
            totalText = "Planned: ${CurrencyUtils.formatBDT(savingsAlloc)}"
        }

        ReportType.FINANCIAL_GOALS -> {
            val goals = uiState.allGoals.filter {
                filterState.selectedGoalStatusFilter == null || it.goal.status == filterState.selectedGoalStatusFilter
            }
            count = goals.size
            val saved = goals.sumOf { it.totalSaved }
            totalText = "Saved: ${CurrencyUtils.formatBDT(saved)}"
        }

        ReportType.LOAN_SUMMARY -> {
            val loans = uiState.allLoans.filter {
                when (filterState.selectedLoanTypeFilter) {
                    LoanTypeFilter.ALL -> true
                    LoanTypeFilter.LENT -> it.type == com.engrshuvo.financemanager.data.model.LoanType.LENT
                    LoanTypeFilter.BORROWED -> it.type == com.engrshuvo.financemanager.data.model.LoanType.BORROWED
                }
            }
            count = loans.size
            val remaining = loans.sumOf { it.remainingAmount }
            totalText = "Outstanding: ${CurrencyUtils.formatBDT(remaining)}"
        }

        ReportType.LOAN_REPAYMENT_HISTORY -> {
            val repayments = uiState.allTransactions.filter {
                (it.categoryId == "loan_repaid" || it.categoryId == "loan_collected") &&
                        it.timestamp in filterState.startDateTimestamp..filterState.endDateTimestamp
            }
            count = repayments.size
            val sum = repayments.sumOf { it.amount }
            totalText = CurrencyUtils.formatBDT(sum)
        }
    }

    return DynamicReportSummary(
        reportTitle = title,
        periodDescription = periodDesc,
        filterDescription = filterDesc,
        recordCount = count,
        totalAmountText = totalText
    )
}
