package com.engrshuvo.financemanager

import com.engrshuvo.financemanager.data.export.PdfReportGenerator
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.GoalCategory
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import com.engrshuvo.financemanager.data.model.GoalPriority
import com.engrshuvo.financemanager.data.model.GoalStatus
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.PdfExportFilterState
import com.engrshuvo.financemanager.data.model.ReportType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocument::class])
class PdfExportUnitTest {

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = 1L,
            type = TransactionType.INCOME,
            amount = 50000.0,
            categoryId = "salary",
            categoryName = "Salary",
            note = "Monthly salary",
            timestamp = System.currentTimeMillis()
        ),
        TransactionEntity(
            id = 2L,
            type = TransactionType.EXPENSE,
            amount = 1500.0,
            categoryId = "groceries",
            categoryName = "Groceries",
            note = "Supermarket shopping",
            timestamp = System.currentTimeMillis()
        )
    )

    private val sampleLoans = listOf(
        LoanEntity(
            id = 10L,
            personName = "John Doe",
            initialAmount = 5000.0,
            remainingAmount = 2000.0,
            type = LoanType.LENT,
            status = LoanStatus.ACTIVE,
            startDate = System.currentTimeMillis()
        )
    )

    private val sampleRepayments = listOf(
        LoanRepaymentEntity(
            id = 100L,
            loanId = 10L,
            amount = 3000.0,
            timestamp = System.currentTimeMillis(),
            note = "Partial repayment"
        )
    )

    private val sampleAllocations = listOf(
        BudgetAllocationEntity(
            monthKey = "2026-10",
            categoryId = "groceries",
            categoryName = "Groceries",
            allocatedAmount = 10000.0
        )
    )

    private val sampleBudgetSetting = BudgetSettingEntity(
        settingKey = BudgetSettingEntity.KEY_MONTHLY_BUDGET,
        amountLimit = 30000.0
    )

    private val sampleGoals = listOf(
        FinancialGoalEntity(
            id = 1L,
            name = "Emergency Fund",
            category = GoalCategory.EMERGENCY,
            targetAmount = 100000.0,
            initialSavedAmount = 20000.0,
            priority = GoalPriority.HIGH,
            status = GoalStatus.ACTIVE
        )
    )

    private val sampleContributions = listOf(
        GoalContributionEntity(
            id = 1L,
            goalId = 1L,
            amount = 5000.0,
            contributionDate = System.currentTimeMillis(),
            note = "Monthly savings deposit"
        )
    )

    @Test
    fun `PdfExportFilterState produces correct file names and month keys`() {
        val filterState = PdfExportFilterState(
            selectedReportType = ReportType.MONTHLY_EXPENSE,
            selectedYear = 2026,
            selectedMonth = 10
        )

        assertEquals("2026-10", filterState.getMonthKey())
        assertEquals("October 2026", filterState.getFormattedMonthYear())
        assertTrue(filterState.getSuggestedFileName().startsWith("Monthly-expense_2026-10"))
        assertTrue(filterState.getSuggestedFileName().endsWith(".pdf"))
    }

    @Test
    fun `generatePdf generates valid non-empty byte stream for all 12 report types`() {
        ReportType.values().forEach { reportType ->
            val filterState = PdfExportFilterState(
                selectedReportType = reportType,
                selectedYear = 2026,
                selectedMonth = 10
            )

            val outputStream = ByteArrayOutputStream()
            PdfReportGenerator.generatePdf(
                filterState = filterState,
                transactions = sampleTransactions,
                loans = sampleLoans,
                repayments = sampleRepayments,
                allocations = sampleAllocations,
                budgetSetting = sampleBudgetSetting,
                goals = sampleGoals,
                goalContributions = sampleContributions,
                outputStream = outputStream
            )

            val bytes = outputStream.toByteArray()
            assertTrue("PDF bytes should be non-empty for report type: ${reportType.name}", bytes.isNotEmpty())
            assertTrue("PDF header should start with %PDF", bytes.size > 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF")
        }
    }

    @Test
    fun `generatePdf handles empty datasets gracefully for all 12 report types`() {
        ReportType.values().forEach { reportType ->
            val filterState = PdfExportFilterState(
                selectedReportType = reportType,
                selectedYear = 2026,
                selectedMonth = 10
            )

            val outputStream = ByteArrayOutputStream()
            PdfReportGenerator.generatePdf(
                filterState = filterState,
                transactions = emptyList(),
                loans = emptyList(),
                repayments = emptyList(),
                allocations = emptyList(),
                budgetSetting = null,
                goals = emptyList(),
                goalContributions = emptyList(),
                outputStream = outputStream
            )

            val bytes = outputStream.toByteArray()
            assertTrue("PDF bytes for empty report should be non-empty for type: ${reportType.name}", bytes.isNotEmpty())
        }
    }

    @Test
    fun `verify suggested file name format for all 12 report types`() {
        ReportType.values().forEach { type ->
            val state = PdfExportFilterState(
                selectedReportType = type,
                selectedYear = 2026,
                selectedMonth = 10
            )
            val fileName = state.getSuggestedFileName()
            assertTrue("File name for ${type.name} should end with .pdf", fileName.endsWith(".pdf"))
            assertTrue("File name for ${type.name} should not be empty", fileName.length > 5)
        }
    }

    @Test
    fun `verify custom date range updates and boundary timestamps`() {
        val state = PdfExportFilterState(
            selectedReportType = ReportType.CUSTOM_DATE_TRANSACTIONS,
            startDateTimestamp = 1700000000000L,
            endDateTimestamp = 1700500000000L
        )
        assertTrue(state.startDateTimestamp <= state.endDateTimestamp)
    }

    @Test
    fun `verify loan type filter and goal status filter states`() {
        val state = PdfExportFilterState(
            selectedReportType = ReportType.LOAN_SUMMARY,
            selectedLoanTypeFilter = com.engrshuvo.financemanager.ui.state.LoanTypeFilter.LENT,
            selectedGoalStatusFilter = GoalStatus.ACTIVE
        )
        assertEquals(com.engrshuvo.financemanager.ui.state.LoanTypeFilter.LENT, state.selectedLoanTypeFilter)
        assertEquals(GoalStatus.ACTIVE, state.selectedGoalStatusFilter)
    }
}
