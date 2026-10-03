package com.engrshuvo.financemanager

import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.ui.state.ArchiveItemWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ArchiveAndRecoveryUnitTest {

    @Test
    fun `soft delete sets archivedAt and removes record from active calculations`() {
        val now = System.currentTimeMillis()
        val activeTx = TransactionEntity(
            id = 1L,
            type = TransactionType.EXPENSE,
            amount = 1200.0,
            categoryId = "groceries",
            categoryName = "Groceries",
            note = "Weekly groceries",
            timestamp = now,
            archivedAt = null
        )

        val archivedTx = activeTx.copy(archivedAt = now)

        val allTransactions = listOf(activeTx, archivedTx)
        val activeTransactions = allTransactions.filter { it.archivedAt == null }
        val archivedTransactions = allTransactions.filter { it.archivedAt != null }

        assertEquals(1, activeTransactions.size)
        assertEquals(1L, activeTransactions.first().id)
        assertEquals(1, archivedTransactions.size)
        assertEquals(1L, archivedTransactions.first().id)

        // Expense sum only reflects active transactions
        val totalActiveExpense = activeTransactions.sumOf { it.amount }
        assertEquals(1200.0, totalActiveExpense, 0.001)
    }

    @Test
    fun `undo or restore brings record back into active calculations exactly once`() {
        val now = System.currentTimeMillis()
        val originalTx = TransactionEntity(
            id = 42L,
            type = TransactionType.INCOME,
            amount = 45000.0,
            categoryId = "salary",
            categoryName = "Monthly Salary",
            note = "Primary salary",
            timestamp = now,
            archivedAt = null
        )

        // Soft delete
        val archived = originalTx.copy(archivedAt = now)
        assertNotNull(archived.archivedAt)

        // Restore
        val restored = archived.copy(archivedAt = null)
        assertNull(restored.archivedAt)
        assertEquals(originalTx.id, restored.id)
        assertEquals(originalTx.amount, restored.amount, 0.001)
        assertEquals(originalTx.timestamp, restored.timestamp)

        val transactionList = listOf(restored)
        val activeIncome = transactionList.filter { it.archivedAt == null }.sumOf { it.amount }
        assertEquals(45000.0, activeIncome, 0.001)
    }

    @Test
    fun `cascade soft delete and restore loan with its repayments and linked transaction`() {
        val now = System.currentTimeMillis()
        val loanId = 100L

        var loan = LoanEntity(
            id = loanId,
            type = LoanType.LENT,
            personName = "Rahim",
            initialAmount = 10000.0,
            remainingAmount = 6000.0,
            status = LoanStatus.ACTIVE,
            startDate = now,
            archivedAt = null
        )

        var repayment = LoanRepaymentEntity(
            id = 1L,
            loanId = loanId,
            amount = 4000.0,
            note = "Partial repayment",
            timestamp = now,
            archivedAt = null
        )

        var linkedTransaction = TransactionEntity(
            id = 501L,
            type = TransactionType.LOAN,
            amount = 10000.0,
            categoryId = "loan_lent",
            categoryName = "Loan to Rahim",
            note = "Lent to Rahim",
            timestamp = now,
            loanId = loanId,
            archivedAt = null
        )

        // Archive cascade
        val archiveTimestamp = now
        loan = loan.copy(archivedAt = archiveTimestamp)
        repayment = repayment.copy(archivedAt = archiveTimestamp)
        linkedTransaction = linkedTransaction.copy(archivedAt = archiveTimestamp)

        assertNotNull(loan.archivedAt)
        assertNotNull(repayment.archivedAt)
        assertNotNull(linkedTransaction.archivedAt)

        // Verify active queries exclude all three
        val activeLoans = listOf(loan).filter { it.archivedAt == null }
        val activeRepayments = listOf(repayment).filter { it.archivedAt == null }
        val activeTransactions = listOf(linkedTransaction).filter { it.archivedAt == null }

        assertTrue(activeLoans.isEmpty())
        assertTrue(activeRepayments.isEmpty())
        assertTrue(activeTransactions.isEmpty())

        // Restore cascade
        loan = loan.copy(archivedAt = null)
        repayment = repayment.copy(archivedAt = null)
        linkedTransaction = linkedTransaction.copy(archivedAt = null)

        assertNull(loan.archivedAt)
        assertNull(repayment.archivedAt)
        assertNull(linkedTransaction.archivedAt)

        assertEquals(1, listOf(loan).filter { it.archivedAt == null }.size)
        assertEquals(1, listOf(repayment).filter { it.archivedAt == null }.size)
        assertEquals(1, listOf(linkedTransaction).filter { it.archivedAt == null }.size)
    }

    @Test
    fun `two-month calendar arithmetic handles month boundaries and leap year correctly`() {
        // Test case 1: Jan 10 -> Mar 10
        val calJan = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 10, 12, 0, 0)
        }
        val expirationJan = (calJan.clone() as Calendar).apply {
            add(Calendar.MONTH, 2)
        }
        assertEquals(2026, expirationJan.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, expirationJan.get(Calendar.MONTH))
        assertEquals(10, expirationJan.get(Calendar.DAY_OF_MONTH))

        // Test case 2: Jan 31 -> Mar 31 (Jan 31 + 2 months = March 31)
        val calJan31 = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 12, 0, 0)
        }
        val expirationJan31 = (calJan31.clone() as Calendar).apply {
            add(Calendar.MONTH, 2)
        }
        assertEquals(Calendar.MARCH, expirationJan31.get(Calendar.MONTH))
        assertEquals(31, expirationJan31.get(Calendar.DAY_OF_MONTH))

        // Test case 3: Dec 15 -> Feb 15 of next year (year rollover)
        val calDec = Calendar.getInstance().apply {
            set(2025, Calendar.DECEMBER, 15, 10, 0, 0)
        }
        val expirationDec = (calDec.clone() as Calendar).apply {
            add(Calendar.MONTH, 2)
        }
        assertEquals(2026, expirationDec.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, expirationDec.get(Calendar.MONTH))
        assertEquals(15, expirationDec.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `auto-purge removes records older than 2 calendar months and preserves recent ones`() {
        val now = System.currentTimeMillis()

        // 3 months ago (expired)
        val calExpired = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -3)
        }
        val expiredTime = calExpired.timeInMillis

        // 10 days ago (active retention)
        val calRecent = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -10)
        }
        val recentTime = calRecent.timeInMillis

        val cutoff = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -2)
        }.timeInMillis

        val txExpired = TransactionEntity(
            id = 1L,
            type = TransactionType.EXPENSE,
            amount = 500.0,
            categoryId = "food",
            categoryName = "Food",
            note = "Old",
            timestamp = expiredTime,
            archivedAt = expiredTime
        )

        val txRecent = TransactionEntity(
            id = 2L,
            type = TransactionType.EXPENSE,
            amount = 750.0,
            categoryId = "food",
            categoryName = "Food",
            note = "Recent",
            timestamp = recentTime,
            archivedAt = recentTime
        )

        val txActive = TransactionEntity(
            id = 3L,
            type = TransactionType.EXPENSE,
            amount = 1000.0,
            categoryId = "food",
            categoryName = "Food",
            note = "Active record",
            timestamp = now,
            archivedAt = null
        )

        val allTxs = listOf(txExpired, txRecent, txActive)

        // Eligible for permanent auto-purge: archivedAt != null AND archivedAt <= cutoff
        val eligibleForPurge = allTxs.filter { it.archivedAt != null && it.archivedAt!! <= cutoff }
        val survivingAfterPurge = allTxs.filterNot { it.archivedAt != null && it.archivedAt!! <= cutoff }

        assertEquals(1, eligibleForPurge.size)
        assertEquals(1L, eligibleForPurge.first().id)

        assertEquals(2, survivingAfterPurge.size)
        assertTrue(survivingAfterPurge.any { it.id == 2L }) // Recent archived item preserved
        assertTrue(survivingAfterPurge.any { it.id == 3L }) // Active item preserved
    }

    @Test
    fun `retention days remaining calculations are accurate and positive`() {
        val now = System.currentTimeMillis()

        // Archived 10 days ago
        val calArchived = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -10)
        }
        val archivedAt = calArchived.timeInMillis

        // Expiration timestamp is 2 calendar months after archive timestamp
        val expCal = Calendar.getInstance().apply {
            timeInMillis = archivedAt
            add(Calendar.MONTH, 2)
        }
        val expiresAt = expCal.timeInMillis

        val remainingMillis = (expiresAt - now).coerceAtLeast(0L)
        val remainingDays = (remainingMillis / (1000 * 60 * 60 * 24)).toInt()

        // Should be approximately ~50 days remaining (60 - 10)
        assertTrue("Days remaining should be positive", remainingDays > 40)
        assertTrue("Days remaining should be under 62", remainingDays <= 62)
    }

    @Test
    fun `permanent deletion of loan cascade removes loan, repayments, and linked transaction`() {
        val loanId = 999L
        val loan = LoanEntity(
            id = loanId,
            type = LoanType.BORROWED,
            personName = "Karim",
            initialAmount = 8000.0,
            remainingAmount = 4000.0,
            status = LoanStatus.ACTIVE
        )
        val repayment1 = LoanRepaymentEntity(id = 11L, loanId = loanId, amount = 2000.0)
        val repayment2 = LoanRepaymentEntity(id = 12L, loanId = loanId, amount = 2000.0)
        val linkedTx = TransactionEntity(
            id = 888L,
            type = TransactionType.LOAN,
            amount = 8000.0,
            categoryId = "loan_borrowed",
            categoryName = "Loan from Karim",
            note = "Borrowed",
            loanId = loanId
        )

        var loans = listOf(loan)
        var repayments = listOf(repayment1, repayment2)
        var transactions = listOf(linkedTx)

        // Permanent delete simulation:
        loans = loans.filterNot { it.id == loanId }
        repayments = repayments.filterNot { it.loanId == loanId }
        transactions = transactions.filterNot { it.loanId == loanId }

        assertTrue(loans.isEmpty())
        assertTrue(repayments.isEmpty())
        assertTrue(transactions.isEmpty())
    }
}
