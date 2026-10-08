package com.engrshuvo.financemanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.FinancialGoalUiModel
import com.engrshuvo.financemanager.data.model.GoalCategory
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import com.engrshuvo.financemanager.data.model.GoalPriority
import com.engrshuvo.financemanager.data.model.GoalStatus
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinancialGoalsAccountingTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            budgetSettingDao = database.budgetSettingDao(),
            loanDao = database.loanDao(),
            budgetAllocationDao = database.budgetAllocationDao(),
            financialGoalDao = database.financialGoalDao(),
            goalContributionDao = database.goalContributionDao(),
            database = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `01 - Invariant - Goal contributions do NOT create ordinary expenses or distort budget`() = runBlocking {
        // Record regular income and regular expense
        val now = System.currentTimeMillis()
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 60000.0,
                categoryId = "salary",
                categoryName = "Salary",
                note = "Monthly salary",
                timestamp = now
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 15000.0,
                categoryId = "food",
                categoryName = "Food & Grocery",
                note = "Daily expenses",
                timestamp = now
            )
        )

        // Create a financial goal: Target 50,000 BDT
        val goalId = repository.insertGoal(
            FinancialGoalEntity(
                name = "New Laptop",
                category = GoalCategory.COMPUTER,
                targetAmount = 50000.0,
                initialSavedAmount = 10000.0,
                priority = GoalPriority.HIGH,
                status = GoalStatus.ACTIVE
            )
        )

        // Add a goal contribution: 5,000 BDT
        repository.addGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = 5000.0,
                contributionDate = now,
                note = "Saved from monthly surplus"
            )
        )

        // Verify active transactions table: exactly 2 transactions (Salary + Food)
        val allTransactions = repository.allTransactions.first()
        assertEquals(2, allTransactions.size)

        // Total expense must remain strictly 15,000 BDT (NOT 20,000 BDT)
        val totalExpense = allTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        assertEquals(15000.0, totalExpense, 0.001)

        // Total income must remain strictly 60,000 BDT
        val totalIncome = allTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        assertEquals(60000.0, totalIncome, 0.001)
    }

    @Test
    fun `02 - Invariant - Goal progress is accurately computed from initial amount and contributions`() = runBlocking {
        val now = System.currentTimeMillis()
        val goalId = repository.insertGoal(
            FinancialGoalEntity(
                name = "New Vehicle",
                category = GoalCategory.VEHICLE,
                targetAmount = 100000.0,
                initialSavedAmount = 25000.0,
                priority = GoalPriority.HIGH,
                status = GoalStatus.ACTIVE
            )
        )

        repository.addGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = 15000.0,
                contributionDate = now,
                note = "First deposit"
            )
        )
        repository.addGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = 10000.0,
                contributionDate = now + 1000,
                note = "Second deposit"
            )
        )

        val goal = repository.getGoalByIdDirect(goalId)
        assertNotNull(goal)

        val contributions = repository.getContributionsForGoal(goalId).first()
        assertEquals(2, contributions.size)

        val totalSaved = goal!!.initialSavedAmount + contributions.sumOf { it.amount }
        assertEquals(50000.0, totalSaved, 0.001) // 25,000 + 15,000 + 10,000 = 50,000 (50%)

        val remaining = (goal.targetAmount - totalSaved).coerceAtLeast(0.0)
        assertEquals(50000.0, remaining, 0.001)

        val progressPercent = (totalSaved / goal.targetAmount) * 100.0
        assertEquals(50.0, progressPercent, 0.001)
    }

    @Test
    fun `03 - Invariant - Target date deadline and required monthly saving calculation`() = runBlocking {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, 5) // 5 months into future
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
        val targetDateMs = cal.timeInMillis

        val goal = FinancialGoalEntity(
            id = 1,
            name = "Family Wedding",
            category = GoalCategory.MARRIAGE,
            targetAmount = 120000.0,
            initialSavedAmount = 20000.0,
            targetDate = targetDateMs,
            priority = GoalPriority.HIGH,
            status = GoalStatus.ACTIVE
        )

        val totalSaved = goal.initialSavedAmount
        val remaining = goal.targetAmount - totalSaved // 100,000 BDT

        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = targetDateMs }
        val diffMonths = ((targetCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)) * 12) +
                (targetCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH))
        val remainingMonths = maxOf(1, diffMonths)

        val requiredMonthly = remaining / remainingMonths
        assertEquals(5, remainingMonths)
        assertEquals(20000.0, requiredMonthly, 0.001) // 100,000 / 5 = 20,000/mo
    }

    @Test
    fun `04 - Invariant - Status transitions between ACTIVE, COMPLETED, PAUSED`() = runBlocking {
        val goalId = repository.insertGoal(
            FinancialGoalEntity(
                name = "Emergency Fund",
                category = GoalCategory.EMERGENCY,
                targetAmount = 30000.0,
                status = GoalStatus.ACTIVE
            )
        )

        var goal = repository.getGoalByIdDirect(goalId)
        assertEquals(GoalStatus.ACTIVE, goal?.status)

        // Pause goal
        repository.updateGoal(goal!!.copy(status = GoalStatus.PAUSED))
        goal = repository.getGoalByIdDirect(goalId)
        assertEquals(GoalStatus.PAUSED, goal?.status)

        // Complete goal
        repository.updateGoal(goal!!.copy(status = GoalStatus.COMPLETED))
        goal = repository.getGoalByIdDirect(goalId)
        assertEquals(GoalStatus.COMPLETED, goal?.status)

        // Reopen goal
        repository.updateGoal(goal!!.copy(status = GoalStatus.ACTIVE))
        goal = repository.getGoalByIdDirect(goalId)
        assertEquals(GoalStatus.ACTIVE, goal?.status)
    }

    @Test
    fun `05 - Invariant - Archiving a goal excludes it from active stream and allows restore`() = runBlocking {
        val goalId = repository.insertGoal(
            FinancialGoalEntity(
                name = "Travel to Coxs Bazar",
                category = GoalCategory.TRAVEL,
                targetAmount = 25000.0,
                initialSavedAmount = 5000.0,
                status = GoalStatus.ACTIVE
            )
        )

        // Add a contribution
        repository.addGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = 2000.0,
                note = "Deposit 1"
            )
        )

        // Active goals must include it
        var activeGoals = repository.activeGoals.first()
        assertEquals(1, activeGoals.size)

        // Archive goal
        repository.archiveGoal(goalId)

        // Active goals must be empty
        activeGoals = repository.activeGoals.first()
        assertEquals(0, activeGoals.size)

        // Archived goals stream must contain it
        val archivedGoals = repository.archivedGoals.first()
        assertEquals(1, archivedGoals.size)
        assertEquals("Travel to Coxs Bazar", archivedGoals[0].name)
        assertNotNull(archivedGoals[0].archivedAt)

        // Restoring brings it back
        repository.restoreGoal(goalId)
        activeGoals = repository.activeGoals.first()
        assertEquals(1, activeGoals.size)
        assertNull(activeGoals[0].archivedAt)
    }

    @Test
    fun `06 - Invariant - Deleting a contribution updates total saved without affecting other records`() = runBlocking {
        val goalId = repository.insertGoal(
            FinancialGoalEntity(
                name = "New Home Savings",
                category = GoalCategory.HOME,
                targetAmount = 500000.0,
                initialSavedAmount = 50000.0
            )
        )

        val contId1 = repository.addGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = 20000.0,
                note = "Bonus deposit"
            )
        )
        val contId2 = repository.addGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = 15000.0,
                note = "Extra savings"
            )
        )

        var contributions = repository.getContributionsForGoal(goalId).first()
        assertEquals(2, contributions.size)

        // Delete contribution 1
        repository.deleteGoalContribution(contId1)

        contributions = repository.getContributionsForGoal(goalId).first()
        assertEquals(1, contributions.size)
        assertEquals(contId2, contributions[0].id)
        assertEquals(15000.0, contributions[0].amount, 0.001)
    }

    @Test
    fun `07 - Invariant - Backup and restore exports and restores goals and contributions intact`() = runBlocking {
        val now = System.currentTimeMillis()
        val goalId = repository.insertGoal(
            FinancialGoalEntity(
                name = "Master Degree",
                category = GoalCategory.EDUCATION,
                targetAmount = 150000.0,
                initialSavedAmount = 30000.0,
                priority = GoalPriority.HIGH,
                status = GoalStatus.ACTIVE
            )
        )
        repository.addGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = 10000.0,
                contributionDate = now,
                note = "Semester saving"
            )
        )

        // Export backup
        val backupData = repository.exportBackupData()
        assertEquals(1, backupData.financialGoals.size)
        assertEquals("Master Degree", backupData.financialGoals[0].name)
        assertEquals(1, backupData.goalContributions.size)
        assertEquals(10000.0, backupData.goalContributions[0].amount, 0.001)

        // JSON serialization and deserialization test
        val jsonString = backupData.toJsonString()
        assertTrue(jsonString.contains("Master Degree"))
        assertTrue(jsonString.contains("EDUCATION"))
        assertTrue(jsonString.contains("Semester saving"))

        val parsed = com.engrshuvo.financemanager.data.model.FinanceBackupData.fromJsonString(jsonString)
        assertNotNull(parsed)
        assertEquals(1, parsed.financialGoals.size)
        assertEquals(1, parsed.goalContributions.size)

        // Restore into database
        val restoreResult = repository.restoreBackupData(parsed)
        assertTrue(restoreResult.isSuccess)

        val restoredGoals = repository.activeGoals.first()
        assertEquals(1, restoredGoals.size)
        assertEquals("Master Degree", restoredGoals[0].name)
    }
}
