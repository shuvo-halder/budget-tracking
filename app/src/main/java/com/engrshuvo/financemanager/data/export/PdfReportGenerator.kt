package com.engrshuvo.financemanager.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.CategoryCatalog
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import com.engrshuvo.financemanager.data.model.GoalStatus
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.PdfExportFilterState
import com.engrshuvo.financemanager.data.model.ReportType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.state.CategoryAllocationUiModel
import com.engrshuvo.financemanager.ui.state.LoanStatusFilter
import com.engrshuvo.financemanager.ui.state.LoanTypeFilter
import com.engrshuvo.financemanager.ui.state.TransactionTypeFilter
import com.engrshuvo.financemanager.ui.util.CurrencyUtils
import com.engrshuvo.financemanager.ui.util.DateUtils
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    // A4 Dimensions in Points (72 points = 1 inch)
    private const val A4_PORTRAIT_WIDTH = 595
    private const val A4_PORTRAIT_HEIGHT = 842

    private const val A4_LANDSCAPE_WIDTH = 842
    private const val A4_LANDSCAPE_HEIGHT = 595

    private const val MARGIN = 36f

    // Color Palette
    private const val COLOR_PRIMARY = 0xFF2563EB.toInt()
    private const val COLOR_HEADER_BG = 0xFF1E293B.toInt()
    private const val COLOR_TEXT_DARK = 0xFF0F172A.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt()
    private const val COLOR_GRID_LINE = 0xFFE2E8F0.toInt()
    private const val COLOR_ROW_ALT = 0xFFF8FAFC.toInt()
    private const val COLOR_INCOME = 0xFF059669.toInt()
    private const val COLOR_EXPENSE = 0xFFDC2626.toInt()
    private const val COLOR_LOAN = 0xFF2563EB.toInt()

    fun generatePdf(
        filterState: PdfExportFilterState,
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        repayments: List<LoanRepaymentEntity>,
        allocations: List<BudgetAllocationEntity>,
        budgetSetting: BudgetSettingEntity?,
        goals: List<FinancialGoalEntity>,
        goalContributions: List<GoalContributionEntity>,
        outputStream: OutputStream
    ) {
        val isLandscape = filterState.selectedReportType.defaultLandscape
        val pageWidth = if (isLandscape) A4_LANDSCAPE_WIDTH else A4_PORTRAIT_WIDTH
        val pageHeight = if (isLandscape) A4_LANDSCAPE_HEIGHT else A4_PORTRAIT_HEIGHT

        val document = PdfDocument()

        val context = PdfPageContext(
            document = document,
            filterState = filterState,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            isLandscape = isLandscape
        )

        context.startNewPage()

        when (filterState.selectedReportType) {
            ReportType.MONTHLY_EXPENSE -> buildMonthlyExpenseReport(context, transactions, allocations)
            ReportType.MONTHLY_INCOME -> buildMonthlyIncomeReport(context, transactions)
            ReportType.DAILY_EXPENSE -> buildDailyExpenseReport(context, transactions)
            ReportType.CATEGORY_EXPENSE -> buildCategoryExpenseReport(context, transactions)
            ReportType.TRANSACTION_HISTORY -> buildTransactionHistoryReport(context, transactions)
            ReportType.MONTHLY_SUMMARY -> buildMonthlySummaryReport(context, transactions, allocations, budgetSetting)
            ReportType.BUDGET_VS_ACTUAL -> buildBudgetVsActualReport(context, allocations, transactions, budgetSetting)
            ReportType.SAVINGS_REPORT -> buildSavingsReport(context, allocations, goals, goalContributions, transactions)
            ReportType.FINANCIAL_GOALS -> buildFinancialGoalsReport(context, goals, goalContributions)
            ReportType.LOAN_SUMMARY -> buildLoanSummaryReport(context, loans, repayments)
            ReportType.LOAN_REPAYMENT_HISTORY -> buildLoanRepaymentHistoryReport(context, loans, repayments)
            ReportType.CUSTOM_DATE_TRANSACTIONS -> buildCustomDateTransactionsReport(context, transactions)
        }

        context.finishDocument()
        document.writeTo(outputStream)
        document.close()
    }

    // ---------------------------------------------------------------------------------------------
    // Page & Layout Helper Class
    // ---------------------------------------------------------------------------------------------
    private class PdfPageContext(
        val document: PdfDocument,
        val filterState: PdfExportFilterState,
        val pageWidth: Int,
        val pageHeight: Int,
        val isLandscape: Boolean
    ) {
        var pageNumber = 0
        private val pages = mutableListOf<PdfDocument.Page>()
        var currentCanvas: Canvas? = null
        var currentY = MARGIN

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_HEADER_BG
            textSize = 18f
            isFakeBoldText = true
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 10f
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 9.5f
        }

        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 9.5f
            isFakeBoldText = true
        }

        val headerCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            isFakeBoldText = true
        }

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRID_LINE
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        fun startNewPage(): Canvas {
            if (currentCanvas != null && pages.isNotEmpty()) {
                drawFooter(currentCanvas!!, pageNumber)
                document.finishPage(pages.last())
            }
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            pages.add(page)
            val canvas = page.canvas
            currentCanvas = canvas
            currentY = MARGIN

            drawDocumentHeader(canvas)
            return canvas
        }

        fun finishDocument() {
            if (currentCanvas != null && pages.isNotEmpty()) {
                drawFooter(currentCanvas!!, pageNumber)
                document.finishPage(pages.last())
            }
        }

        fun ensureSpace(neededHeight: Float): Canvas {
            val canvas = currentCanvas ?: startNewPage()
            if (currentY + neededHeight > pageHeight - MARGIN - 25f) {
                return startNewPage()
            }
            return canvas
        }

        private fun drawDocumentHeader(canvas: Canvas) {
            // Header Top Bar
            canvas.drawRect(MARGIN, currentY, pageWidth - MARGIN, currentY + 36f, Paint().apply {
                color = COLOR_HEADER_BG
            })

            // App Name & Report Category Badge
            val appTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 12f
                isFakeBoldText = true
            }
            canvas.drawText("DAILY BUDGET & LOAN MANAGER", MARGIN + 10f, currentY + 22f, appTitlePaint)

            val categoryTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY
                textSize = 9f
                isFakeBoldText = true
            }
            val reportCatText = filterState.selectedReportType.category.displayName.uppercase()
            canvas.drawText(reportCatText, pageWidth - MARGIN - categoryTagPaint.measureText(reportCatText) - 10f, currentY + 22f, categoryTagPaint)

            currentY += 46f

            // Title & Subtitle
            canvas.drawText(filterState.selectedReportType.title, MARGIN, currentY + 16f, titlePaint)
            currentY += 22f

            val periodText = when (filterState.selectedReportType) {
                ReportType.MONTHLY_EXPENSE,
                ReportType.MONTHLY_INCOME,
                ReportType.CATEGORY_EXPENSE,
                ReportType.MONTHLY_SUMMARY,
                ReportType.BUDGET_VS_ACTUAL,
                ReportType.SAVINGS_REPORT -> "Period: ${filterState.getFormattedMonthYear()}"

                ReportType.DAILY_EXPENSE -> "Period: ${DateUtils.formatShortDate(filterState.selectedDailyTimestamp)}"

                ReportType.TRANSACTION_HISTORY,
                ReportType.LOAN_REPAYMENT_HISTORY,
                ReportType.CUSTOM_DATE_TRANSACTIONS -> "Period: ${DateUtils.formatShortDate(filterState.startDateTimestamp)} to ${DateUtils.formatShortDate(filterState.endDateTimestamp)}"

                ReportType.LOAN_SUMMARY,
                ReportType.FINANCIAL_GOALS -> "As of ${DateUtils.formatShortDate(System.currentTimeMillis())}"
            }

            val genTimeText = "Generated: ${SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date())} | BDT (৳)"
            canvas.drawText(periodText, MARGIN, currentY + 10f, subtitlePaint)
            canvas.drawText(genTimeText, pageWidth - MARGIN - subtitlePaint.measureText(genTimeText), currentY + 10f, subtitlePaint)

            currentY += 18f
            canvas.drawLine(MARGIN, currentY, pageWidth - MARGIN, currentY, gridPaint)
            currentY += 12f
        }

        private fun drawFooter(canvas: Canvas, pageNum: Int) {
            val footerY = pageHeight - MARGIN + 12f
            canvas.drawLine(MARGIN, footerY - 10f, pageWidth - MARGIN, footerY - 10f, gridPaint)

            val footerText = "Confidential Financial Report • Daily Budget & Loan Manager"
            canvas.drawText(footerText, MARGIN, footerY, subtitlePaint)

            val pageText = "Page $pageNum"
            canvas.drawText(pageText, pageWidth - MARGIN - subtitlePaint.measureText(pageText), footerY, subtitlePaint)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helper: Draw Summary KPI Cards
    // ---------------------------------------------------------------------------------------------
    private fun drawKpiRow(
        context: PdfPageContext,
        kpis: List<Pair<String, String>>
    ) {
        if (kpis.isEmpty()) return
        context.ensureSpace(50f)
        val canvas = context.currentCanvas!!

        val usableWidth = context.pageWidth - (MARGIN * 2)
        val cardWidth = (usableWidth - (8f * (kpis.size - 1))) / kpis.size
        val cardHeight = 42f

        var x = MARGIN
        val y = context.currentY

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROW_ALT
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRID_LINE
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        for ((title, valText) in kpis) {
            val rect = RectF(x, y, x + cardWidth, y + cardHeight)
            canvas.drawRoundRect(rect, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(rect, 6f, 6f, cardBorderPaint)

            canvas.drawText(title.uppercase(), x + 8f, y + 14f, context.subtitlePaint)
            canvas.drawText(valText, x + 8f, y + 32f, context.bodyBoldPaint)

            x += cardWidth + 8f
        }

        context.currentY += cardHeight + 14f
    }

    // ---------------------------------------------------------------------------------------------
    // Helper: Draw Table
    // ---------------------------------------------------------------------------------------------
    private fun drawTable(
        context: PdfPageContext,
        headers: List<String>,
        columnWidths: List<Float>,
        rows: List<List<String>>,
        alignRightCols: Set<Int> = emptySet()
    ) {
        val usableWidth = context.pageWidth - (MARGIN * 2)
        val headerHeight = 22f
        val rowHeight = 20f

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
        }
        val altRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROW_ALT
            style = Paint.Style.FILL
        }

        fun drawHeaderRow(canvas: Canvas, y: Float) {
            canvas.drawRect(MARGIN, y, MARGIN + usableWidth, y + headerHeight, headerBgPaint)
            var x = MARGIN
            for (i in headers.indices) {
                val colWidth = columnWidths[i]
                val text = headers[i]
                val textWidth = context.headerCellPaint.measureText(text)
                val isRight = alignRightCols.contains(i)
                val drawX = if (isRight) x + colWidth - textWidth - 6f else x + 6f
                canvas.drawText(text, drawX, y + 15f, context.headerCellPaint)
                x += colWidth
            }
        }

        var canvas = context.ensureSpace(headerHeight + rowHeight)
        drawHeaderRow(canvas, context.currentY)
        context.currentY += headerHeight

        for (rowIndex in rows.indices) {
            canvas = context.ensureSpace(rowHeight)
            if (context.currentY == MARGIN + 46f + 22f + 18f + 12f) { // If new page was just started
                drawHeaderRow(canvas, context.currentY)
                context.currentY += headerHeight
            }

            val row = rows[rowIndex]
            val y = context.currentY

            if (rowIndex % 2 == 1) {
                canvas.drawRect(MARGIN, y, MARGIN + usableWidth, y + rowHeight, altRowPaint)
            }

            var x = MARGIN
            for (i in row.indices) {
                val colWidth = columnWidths[i]
                val rawText = row[i]
                val isRight = alignRightCols.contains(i)

                // Truncate text if it exceeds cell width
                val maxTextWidth = colWidth - 12f
                var text = rawText
                if (context.bodyPaint.measureText(text) > maxTextWidth && text.length > 3) {
                    while (text.length > 3 && context.bodyPaint.measureText("$text...") > maxTextWidth) {
                        text = text.substring(0, text.length - 1)
                    }
                    text = "$text..."
                }

                val textWidth = context.bodyPaint.measureText(text)
                val drawX = if (isRight) x + colWidth - textWidth - 6f else x + 6f
                canvas.drawText(text, drawX, y + 14f, context.bodyPaint)
                x += colWidth
            }

            canvas.drawLine(MARGIN, y + rowHeight, MARGIN + usableWidth, y + rowHeight, context.gridPaint)
            context.currentY += rowHeight
        }

        context.currentY += 10f
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 1: Monthly Expense Report
    // ---------------------------------------------------------------------------------------------
    private fun buildMonthlyExpenseReport(
        context: PdfPageContext,
        allTransactions: List<TransactionEntity>,
        allocations: List<BudgetAllocationEntity>
    ) {
        val startMs = PdfExportFilterState.getStartOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)
        val endMs = PdfExportFilterState.getEndOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)

        val expenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.timestamp in startMs..endMs &&
                    (context.filterState.selectedCategoryFilterId == null || it.categoryId == context.filterState.selectedCategoryFilterId)
        }.sortedBy { it.timestamp }

        val totalExpense = expenses.sumOf { it.amount }

        drawKpiRow(
            context,
            listOf(
                "Total Expenses" to CurrencyUtils.formatBDT(totalExpense),
                "Expense Entries" to "${expenses.size} records",
                "Category Count" to "${expenses.map { it.categoryId }.distinct().size} categories"
            )
        )

        val headers = listOf("Date", "Category", "Description / Note", "Amount (৳)")
        val columnWidths = listOf(90f, 130f, 183f, 120f)
        val rows = expenses.map {
            listOf(
                DateUtils.formatShortDate(it.timestamp),
                it.categoryName,
                it.note.ifBlank { "-" },
                CurrencyUtils.formatBDT(it.amount)
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 2: Monthly Income Report
    // ---------------------------------------------------------------------------------------------
    private fun buildMonthlyIncomeReport(
        context: PdfPageContext,
        allTransactions: List<TransactionEntity>
    ) {
        val startMs = PdfExportFilterState.getStartOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)
        val endMs = PdfExportFilterState.getEndOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)

        val incomes = allTransactions.filter {
            it.type == TransactionType.INCOME &&
                    it.timestamp in startMs..endMs &&
                    (context.filterState.selectedCategoryFilterId == null || it.categoryId == context.filterState.selectedCategoryFilterId)
        }.sortedBy { it.timestamp }

        val totalIncome = incomes.sumOf { it.amount }

        drawKpiRow(
            context,
            listOf(
                "Total Income" to CurrencyUtils.formatBDT(totalIncome),
                "Income Receipts" to "${incomes.size} records",
                "Primary Source" to (incomes.maxByOrNull { it.amount }?.categoryName ?: "-")
            )
        )

        val headers = listOf("Date", "Category / Source", "Note", "Amount (৳)")
        val columnWidths = listOf(90f, 140f, 173f, 120f)
        val rows = incomes.map {
            listOf(
                DateUtils.formatShortDate(it.timestamp),
                it.categoryName,
                it.note.ifBlank { "-" },
                CurrencyUtils.formatBDT(it.amount)
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 3: Daily Expense Report
    // ---------------------------------------------------------------------------------------------
    private fun buildDailyExpenseReport(
        context: PdfPageContext,
        allTransactions: List<TransactionEntity>
    ) {
        val startMs = DateUtils.getStartOfDay(context.filterState.selectedDailyTimestamp)
        val endMs = DateUtils.getEndOfDay(context.filterState.selectedDailyTimestamp)

        val expenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.timestamp in startMs..endMs &&
                    (context.filterState.selectedCategoryFilterId == null || it.categoryId == context.filterState.selectedCategoryFilterId)
        }.sortedBy { it.timestamp }

        val totalExpense = expenses.sumOf { it.amount }

        drawKpiRow(
            context,
            listOf(
                "Daily Expense Total" to CurrencyUtils.formatBDT(totalExpense),
                "Transactions Today" to "${expenses.size} items",
                "Date" to DateUtils.formatShortDate(context.filterState.selectedDailyTimestamp)
            )
        )

        val headers = listOf("Time", "Category", "Note / Item", "Amount (৳)")
        val columnWidths = listOf(90f, 130f, 183f, 120f)
        val rows = expenses.map {
            listOf(
                SimpleDateFormat("hh:mm a", Locale.US).format(Date(it.timestamp)),
                it.categoryName,
                it.note.ifBlank { "-" },
                CurrencyUtils.formatBDT(it.amount)
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 4: Category-wise Expense Report
    // ---------------------------------------------------------------------------------------------
    private fun buildCategoryExpenseReport(
        context: PdfPageContext,
        allTransactions: List<TransactionEntity>
    ) {
        val startMs = PdfExportFilterState.getStartOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)
        val endMs = PdfExportFilterState.getEndOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)

        val expenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.timestamp in startMs..endMs &&
                    (context.filterState.selectedCategoryFilterId == null || it.categoryId == context.filterState.selectedCategoryFilterId)
        }

        val totalExpense = expenses.sumOf { it.amount }

        val categoryGroups = expenses.groupBy { it.categoryId }.map { (catId, list) ->
            val catName = list.firstOrNull()?.categoryName ?: CategoryCatalog.getCategoryById(catId).name
            val amount = list.sumOf { it.amount }
            val percent = if (totalExpense > 0) (amount / totalExpense) * 100.0 else 0.0
            Triple(catName, amount, percent)
        }.sortedByDescending { it.second }

        drawKpiRow(
            context,
            listOf(
                "Total Monthly Expense" to CurrencyUtils.formatBDT(totalExpense),
                "Active Categories" to "${categoryGroups.size} categories",
                "Largest Category" to (categoryGroups.firstOrNull()?.first ?: "-")
            )
        )

        val headers = listOf("Category Name", "Total Spent (৳)", "% of Total", "Transaction Count")
        val columnWidths = listOf(180f, 120f, 100f, 123f)

        val rows = categoryGroups.map { (catName, amount, percent) ->
            val count = expenses.count { it.categoryName == catName }
            listOf(
                catName,
                CurrencyUtils.formatBDT(amount),
                "${String.format("%.1f", percent)}%",
                "$count items"
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(1, 2, 3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 5: Transaction History (Landscape-friendly)
    // ---------------------------------------------------------------------------------------------
    private fun buildTransactionHistoryReport(
        context: PdfPageContext,
        allTransactions: List<TransactionEntity>
    ) {
        val filter = context.filterState
        val filtered = allTransactions.filter {
            it.timestamp in filter.startDateTimestamp..filter.endDateTimestamp &&
                    (filter.selectedCategoryFilterId == null || it.categoryId == filter.selectedCategoryFilterId) &&
                    when (filter.selectedTransactionTypeFilter) {
                        TransactionTypeFilter.ALL -> true
                        TransactionTypeFilter.INCOME -> it.type == TransactionType.INCOME
                        TransactionTypeFilter.EXPENSE -> it.type == TransactionType.EXPENSE
                        TransactionTypeFilter.LOAN -> it.type == TransactionType.LOAN
                    }
        }.sortedBy { it.timestamp }

        val totalIncome = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        drawKpiRow(
            context,
            listOf(
                "Total Income" to CurrencyUtils.formatBDT(totalIncome),
                "Total Expenses" to CurrencyUtils.formatBDT(totalExpense),
                "Net Difference" to CurrencyUtils.formatBDT(totalIncome - totalExpense),
                "Record Count" to "${filtered.size} records"
            )
        )

        val usableWidth = context.pageWidth - (MARGIN * 2)
        val headers = listOf("Date", "Type", "Category", "Note / Description", "Amount (৳)")
        val columnWidths = listOf(100f, 80f, 150f, usableWidth - 440f, 110f)

        val rows = filtered.map {
            listOf(
                DateUtils.formatShortDate(it.timestamp),
                it.type.name,
                it.categoryName,
                it.note.ifBlank { "-" },
                CurrencyUtils.formatBDT(it.amount)
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(4))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 6: Monthly Financial Summary
    // ---------------------------------------------------------------------------------------------
    private fun buildMonthlySummaryReport(
        context: PdfPageContext,
        allTransactions: List<TransactionEntity>,
        allocations: List<BudgetAllocationEntity>,
        budgetSetting: BudgetSettingEntity?
    ) {
        val monthKey = context.filterState.getMonthKey()
        val startMs = PdfExportFilterState.getStartOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)
        val endMs = PdfExportFilterState.getEndOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)

        val monthTxs = allTransactions.filter { it.timestamp in startMs..endMs }
        val income = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netCash = income - expense

        val monthAllocations = allocations.filter { it.monthKey == monthKey }
        val totalAllocated = monthAllocations.sumOf { it.allocatedAmount }
        val unallocated = maxOf(0.0, income - totalAllocated)

        drawKpiRow(
            context,
            listOf(
                "Total Monthly Income" to CurrencyUtils.formatBDT(income),
                "Total Actual Expenses" to CurrencyUtils.formatBDT(expense),
                "Net Cash Change" to CurrencyUtils.formatBDT(netCash),
                "Total Allocated Budget" to CurrencyUtils.formatBDT(totalAllocated)
            )
        )

        val headers = listOf("Metric / Financial Dimension", "Status / Notes", "Amount (৳)")
        val columnWidths = listOf(220f, 183f, 120f)

        val rows = listOf(
            listOf("Ordinary Income Receipts", "Salary & earnings", CurrencyUtils.formatBDT(income)),
            listOf("Ordinary Actual Expenses", "Food, rent, bills, daily living", CurrencyUtils.formatBDT(expense)),
            listOf("Net Monthly Cashflow", if (netCash >= 0) "Surplus" else "Deficit", CurrencyUtils.formatBDT(netCash)),
            listOf("Planned Budget Allocations", "Target allocation across categories", CurrencyUtils.formatBDT(totalAllocated)),
            listOf("Unallocated Salary Balance", "Income remaining to allocate", CurrencyUtils.formatBDT(unallocated))
        )

        drawTable(context, headers, columnWidths, rows, setOf(2))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 7: Budget vs Actual Report
    // ---------------------------------------------------------------------------------------------
    private fun buildBudgetVsActualReport(
        context: PdfPageContext,
        allocations: List<BudgetAllocationEntity>,
        allTransactions: List<TransactionEntity>,
        budgetSetting: BudgetSettingEntity?
    ) {
        val monthKey = context.filterState.getMonthKey()
        val startMs = PdfExportFilterState.getStartOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)
        val endMs = PdfExportFilterState.getEndOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)

        val monthAllocations = allocations.filter { it.monthKey == monthKey }
        val monthExpenses = allTransactions.filter { it.type == TransactionType.EXPENSE && it.timestamp in startMs..endMs }

        val totalAllocated = monthAllocations.sumOf { it.allocatedAmount }
        val totalActualSpent = monthExpenses.sumOf { it.amount }

        drawKpiRow(
            context,
            listOf(
                "Total Planned Budget" to CurrencyUtils.formatBDT(totalAllocated),
                "Total Actual Spent" to CurrencyUtils.formatBDT(totalActualSpent),
                "Budget Variance" to CurrencyUtils.formatBDT(totalAllocated - totalActualSpent)
            )
        )

        val headers = listOf("Category Name", "Planned Target (৳)", "Actual Spent (৳)", "Remaining / Variance", "Status")
        val columnWidths = listOf(140f, 100f, 100f, 103f, 80f)

        val rows = CategoryCatalog.getAllCategories().map { category ->
            val alloc = monthAllocations.find { it.categoryId == category.id }?.allocatedAmount ?: 0.0
            val spent = monthExpenses.filter { it.categoryId == category.id }.sumOf { it.amount }
            val variance = alloc - spent
            val status = when {
                alloc == 0.0 && spent > 0.0 -> "Unbudgeted"
                spent > alloc -> "Over Budget"
                spent == alloc && alloc > 0 -> "Exact"
                else -> "On Track"
            }
            listOf(
                category.name,
                CurrencyUtils.formatBDT(alloc),
                CurrencyUtils.formatBDT(spent),
                CurrencyUtils.formatBDT(variance),
                status
            )
        }.filter { it[1] != "৳0" || it[2] != "৳0" } // Only display active categories

        drawTable(context, headers, columnWidths, rows, setOf(1, 2, 3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 8: Savings Report
    // ---------------------------------------------------------------------------------------------
    private fun buildSavingsReport(
        context: PdfPageContext,
        allocations: List<BudgetAllocationEntity>,
        goals: List<FinancialGoalEntity>,
        contributions: List<GoalContributionEntity>,
        allTransactions: List<TransactionEntity>
    ) {
        val monthKey = context.filterState.getMonthKey()
        val startMs = PdfExportFilterState.getStartOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)
        val endMs = PdfExportFilterState.getEndOfMonthTimestamp(context.filterState.selectedYear, context.filterState.selectedMonth)

        val monthAllocations = allocations.filter { it.monthKey == monthKey }
        val plannedSavings = monthAllocations.filter { it.categoryId == "savings" || it.categoryId == "emergency" }.sumOf { it.allocatedAmount }

        val monthContributions = contributions.filter { it.contributionDate in startMs..endMs }
        val actualGoalDeposits = monthContributions.sumOf { it.amount }

        drawKpiRow(
            context,
            listOf(
                "Planned Budget Savings" to CurrencyUtils.formatBDT(plannedSavings),
                "Actual Goal Deposits" to CurrencyUtils.formatBDT(actualGoalDeposits),
                "Deposit Records" to "${monthContributions.size} deposits"
            )
        )

        val headers = listOf("Deposit Date", "Goal Name", "Deposit Note", "Amount (৳)")
        val columnWidths = listOf(90f, 140f, 173f, 120f)

        val rows = monthContributions.map { contrib ->
            val goalName = goals.find { it.id == contrib.goalId }?.name ?: "Financial Goal #${contrib.goalId}"
            listOf(
                DateUtils.formatShortDate(contrib.contributionDate),
                goalName,
                contrib.note.ifBlank { "Savings contribution deposit" },
                CurrencyUtils.formatBDT(contrib.amount)
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 9: Financial Goals Report
    // ---------------------------------------------------------------------------------------------
    private fun buildFinancialGoalsReport(
        context: PdfPageContext,
        goals: List<FinancialGoalEntity>,
        contributions: List<GoalContributionEntity>
    ) {
        val filteredGoals = goals.filter { goal ->
            context.filterState.selectedGoalStatusFilter == null || goal.status == context.filterState.selectedGoalStatusFilter
        }

        val totalTarget = filteredGoals.sumOf { it.targetAmount }
        val totalSaved = filteredGoals.sumOf { goal ->
            val goalContribs = contributions.filter { it.goalId == goal.id }.sumOf { it.amount }
            goal.initialSavedAmount + goalContribs
        }

        drawKpiRow(
            context,
            listOf(
                "Total Target Pool" to CurrencyUtils.formatBDT(totalTarget),
                "Total Saved Pool" to CurrencyUtils.formatBDT(totalSaved),
                "Overall Progress" to "${if (totalTarget > 0) String.format("%.1f", (totalSaved / totalTarget) * 100.0) else "0"}%",
                "Goal Count" to "${filteredGoals.size} targets"
            )
        )

        val headers = listOf("Goal Name", "Category", "Priority", "Target (৳)", "Saved (৳)", "Progress", "Status")
        val columnWidths = listOf(115f, 75f, 50f, 75f, 75f, 55f, 78f)

        val rows = filteredGoals.map { goal ->
            val goalContribs = contributions.filter { it.goalId == goal.id }.sumOf { it.amount }
            val saved = goal.initialSavedAmount + goalContribs
            val progress = if (goal.targetAmount > 0) (saved / goal.targetAmount) * 100.0 else 0.0
            listOf(
                goal.name,
                goal.category.displayName,
                goal.priority.displayName,
                CurrencyUtils.formatBDT(goal.targetAmount),
                CurrencyUtils.formatBDT(saved),
                "${String.format("%.0f", progress)}%",
                goal.status.displayName
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(3, 4, 5))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 10: Loan Summary Report
    // ---------------------------------------------------------------------------------------------
    private fun buildLoanSummaryReport(
        context: PdfPageContext,
        loans: List<LoanEntity>,
        repayments: List<LoanRepaymentEntity>
    ) {
        val filteredLoans = loans.filter { loan ->
            when (context.filterState.selectedLoanTypeFilter) {
                LoanTypeFilter.ALL -> true
                LoanTypeFilter.LENT -> loan.type == LoanType.LENT
                LoanTypeFilter.BORROWED -> loan.type == LoanType.BORROWED
            }
        }

        val activeLent = filteredLoans.filter { it.type == LoanType.LENT && it.status == LoanStatus.ACTIVE }.sumOf { it.remainingAmount }
        val activeBorrowed = filteredLoans.filter { it.type == LoanType.BORROWED && it.status == LoanStatus.ACTIVE }.sumOf { it.remainingAmount }

        drawKpiRow(
            context,
            listOf(
                "Outstanding Lent" to CurrencyUtils.formatBDT(activeLent),
                "Outstanding Borrowed" to CurrencyUtils.formatBDT(activeBorrowed),
                "Total Loan Records" to "${filteredLoans.size} records"
            )
        )

        val headers = listOf("Person / Party", "Type", "Initial (৳)", "Remaining (৳)", "Start Date", "Status")
        val columnWidths = listOf(130f, 70f, 95f, 95f, 80f, 53f)

        val rows: List<List<String>> = filteredLoans.map { loan ->
            listOf(
                loan.personName,
                loan.type.name,
                CurrencyUtils.formatBDT(loan.initialAmount),
                CurrencyUtils.formatBDT(loan.remainingAmount),
                DateUtils.formatShortDate(loan.startDate),
                loan.status.name
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(2, 3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 11: Loan Repayment History
    // ---------------------------------------------------------------------------------------------
    private fun buildLoanRepaymentHistoryReport(
        context: PdfPageContext,
        loans: List<LoanEntity>,
        repayments: List<LoanRepaymentEntity>
    ) {
        val filter = context.filterState
        val filteredReps = repayments.filter { rep ->
            rep.timestamp in filter.startDateTimestamp..filter.endDateTimestamp
        }.sortedBy { it.timestamp }

        val totalRepayments = filteredReps.sumOf { it.amount }

        drawKpiRow(
            context,
            listOf(
                "Total Repayments" to CurrencyUtils.formatBDT(totalRepayments),
                "Repayment Count" to "${filteredReps.size} records"
            )
        )

        val headers = listOf("Date", "Loan / Person", "Repayment Note", "Amount (৳)")
        val columnWidths = listOf(90f, 140f, 173f, 120f)

        val rows = filteredReps.map { rep ->
            val loan = loans.find { it.id == rep.loanId }
            val personText = loan?.let { "${it.personName} (${it.type.name})" } ?: "Loan #${rep.loanId}"
            listOf(
                DateUtils.formatShortDate(rep.timestamp),
                personText,
                rep.note.ifBlank { "Loan repayment deposit" },
                CurrencyUtils.formatBDT(rep.amount)
            )
        }

        drawTable(context, headers, columnWidths, rows, setOf(3))
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT BUILDER 12: Custom Date Range Transactions Report
    // ---------------------------------------------------------------------------------------------
    private fun buildCustomDateTransactionsReport(
        context: PdfPageContext,
        allTransactions: List<TransactionEntity>
    ) {
        buildTransactionHistoryReport(context, allTransactions)
    }
}
