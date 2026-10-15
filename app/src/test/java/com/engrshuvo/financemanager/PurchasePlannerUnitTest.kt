package com.engrshuvo.financemanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.model.PurchaseItemEntity
import com.engrshuvo.financemanager.data.model.TransactionEntity
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.ui.state.MoreSubDestination
import com.engrshuvo.financemanager.ui.state.PurchaseItemFilter
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PurchasePlannerUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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
            purchaseItemDao = database.purchaseItemDao(),
            database = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `newItemSaved_appearsInPendingAndAllFlows`() = runBlocking {
        val item = PurchaseItemEntity(
            title = "Whole Milk & Organic Eggs",
            note = "2 bottles of milk, 1 dozen eggs",
            isCompleted = false,
            createdAt = 1700000000000L
        )

        val id = repository.insertPurchaseItem(item)
        assertTrue(id > 0)

        val allItems = repository.allPurchaseItems.first()
        assertEquals(1, allItems.size)
        val saved = allItems[0]
        assertEquals(id, saved.id)
        assertEquals("Whole Milk & Organic Eggs", saved.title)
        assertEquals("2 bottles of milk, 1 dozen eggs", saved.note)
        assertFalse(saved.isCompleted)
        assertNull(saved.completedAt)

        val pendingFromDao = database.purchaseItemDao().getPendingItemsFlow().first()
        assertEquals(1, pendingFromDao.size)
        assertEquals(id, pendingFromDao[0].id)

        val completedFromDao = database.purchaseItemDao().getCompletedItemsFlow().first()
        assertTrue(completedFromDao.isEmpty())
    }

    @Test
    fun `editingItem_persistsChangesCorrectly`() = runBlocking {
        val original = PurchaseItemEntity(
            title = "Notebook",
            note = "A5 dotted",
            isCompleted = false
        )
        val id = repository.insertPurchaseItem(original)
        val fetched = database.purchaseItemDao().getItemById(id)
        assertNotNull(fetched)

        val modified = fetched!!.copy(
            title = "Notebook & Gel Pens",
            note = "A5 dotted and 0.5mm black pens"
        )
        val updated = repository.updatePurchaseItem(modified)
        assertTrue(updated)

        val refetched = database.purchaseItemDao().getItemById(id)
        assertNotNull(refetched)
        assertEquals("Notebook & Gel Pens", refetched!!.title)
        assertEquals("A5 dotted and 0.5mm black pens", refetched.note)
        assertFalse(refetched.isCompleted)
    }

    @Test
    fun `markingItemCompleted_movesOutOfPendingIntoCompleted`() = runBlocking {
        val id = repository.insertPurchaseItem(
            PurchaseItemEntity(title = "Toothpaste", isCompleted = false)
        )

        // Verify initially pending
        assertEquals(1, database.purchaseItemDao().getPendingItemsFlow().first().size)
        assertEquals(0, database.purchaseItemDao().getCompletedItemsFlow().first().size)

        // Mark completed
        val marked = repository.markPurchaseItemCompleted(id)
        assertTrue(marked)

        // Verify now in completed and gone from pending
        val pending = database.purchaseItemDao().getPendingItemsFlow().first()
        val completed = database.purchaseItemDao().getCompletedItemsFlow().first()

        assertEquals(0, pending.size)
        assertEquals(1, completed.size)
        assertEquals(id, completed[0].id)
        assertTrue(completed[0].isCompleted)
        assertNotNull(completed[0].completedAt)
    }

    @Test
    fun `restoringCompletedItem_returnsItToPending`() = runBlocking {
        val id = repository.insertPurchaseItem(
            PurchaseItemEntity(title = "Apples", isCompleted = false)
        )
        repository.markPurchaseItemCompleted(id)
        assertEquals(1, database.purchaseItemDao().getCompletedItemsFlow().first().size)

        // Restore to pending
        val restored = repository.restorePurchaseItemToPending(id)
        assertTrue(restored)

        val pending = database.purchaseItemDao().getPendingItemsFlow().first()
        val completed = database.purchaseItemDao().getCompletedItemsFlow().first()

        assertEquals(1, pending.size)
        assertEquals(0, completed.size)
        val item = pending[0]
        assertEquals(id, item.id)
        assertFalse(item.isCompleted)
        assertNull(item.completedAt)
    }

    @Test
    fun `deletingItem_removesItPermanently`() = runBlocking {
        val id = repository.insertPurchaseItem(
            PurchaseItemEntity(title = "Bread", isCompleted = false)
        )
        assertEquals(1, repository.allPurchaseItems.first().size)

        val deleted = repository.deletePurchaseItem(id)
        assertTrue(deleted)

        val allItems = repository.allPurchaseItems.first()
        assertTrue(allItems.isEmpty())
        assertNull(database.purchaseItemDao().getItemById(id))
    }

    @Test
    fun `recordsSurviveDatabaseReload`() = runBlocking {
        val id1 = repository.insertPurchaseItem(PurchaseItemEntity(title = "Shampoo", note = "Anti-dandruff"))
        val id2 = repository.insertPurchaseItem(PurchaseItemEntity(title = "Body Wash", note = "Lavender"))
        repository.markPurchaseItemCompleted(id2)

        // Create a new repository referencing the same database to simulate app/repo restart
        val reloadedRepo = FinanceRepository(
            transactionDao = database.transactionDao(),
            budgetSettingDao = database.budgetSettingDao(),
            loanDao = database.loanDao(),
            budgetAllocationDao = database.budgetAllocationDao(),
            financialGoalDao = database.financialGoalDao(),
            goalContributionDao = database.goalContributionDao(),
            purchaseItemDao = database.purchaseItemDao(),
            database = database
        )

        val allItems = reloadedRepo.allPurchaseItems.first()
        assertEquals(2, allItems.size)

        val item1 = allItems.find { it.id == id1 }
        val item2 = allItems.find { it.id == id2 }

        assertNotNull(item1)
        assertNotNull(item2)
        assertEquals("Shampoo", item1!!.title)
        assertFalse(item1.isCompleted)
        assertEquals("Body Wash", item2!!.title)
        assertTrue(item2.isCompleted)
    }

    @Test
    fun `strictFinancialIsolation_completingRestoringDeletingNeverChangesBalanceOrCreatesTransactions`() = runBlocking {
        // 1. Establish an initial financial state: 10,000 Income and 2,500 Expense => Balance = 7,500
        val incomeTx = TransactionEntity(
            type = TransactionType.INCOME,
            amount = 10000.0,
            categoryId = "salary",
            categoryName = "Salary",
            note = "Base Monthly Salary",
            timestamp = 1700000000000L
        )
        val expenseTx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = 2500.0,
            categoryId = "food",
            categoryName = "Food",
            note = "Groceries",
            timestamp = 1700001000000L
        )
        database.transactionDao().insertTransaction(incomeTx)
        database.transactionDao().insertTransaction(expenseTx)

        val initialTransactions = repository.allTransactions.first()
        assertEquals(2, initialTransactions.size)
        val initialIncome = initialTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val initialExpense = initialTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val initialBalance = initialIncome - initialExpense
        assertEquals(7500.0, initialBalance, 0.001)

        // 2. Perform Purchase Planner operations
        val p1 = repository.insertPurchaseItem(PurchaseItemEntity(title = "New Headphones", note = "Budget 3000"))
        val p2 = repository.insertPurchaseItem(PurchaseItemEntity(title = "Office Chair", note = "Ergonomic"))

        // Complete p1
        repository.markPurchaseItemCompleted(p1)

        // Restore p1
        repository.restorePurchaseItemToPending(p1)

        // Delete p2
        repository.deletePurchaseItem(p2)

        // Re-complete p1
        repository.markPurchaseItemCompleted(p1)

        // 3. Verify financial isolation: transactions count, income, expense, and balance are 100% untouched
        val postTransactions = repository.allTransactions.first()
        assertEquals(2, postTransactions.size)
        val postIncome = postTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val postExpense = postTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val postBalance = postIncome - postExpense

        assertEquals(10000.0, postIncome, 0.001)
        assertEquals(2500.0, postExpense, 0.001)
        assertEquals(7500.0, postBalance, 0.001)
        assertEquals(initialBalance, postBalance, 0.001)
    }

    @Test
    fun `searchAndFilters_filterLogicValidation`() {
        val items = listOf(
            PurchaseItemEntity(id = 1, title = "Apples", note = "Fuji apples 1kg", isCompleted = false, createdAt = 1000L),
            PurchaseItemEntity(id = 2, title = "Bananas", note = "Yellow ripe", isCompleted = true, createdAt = 2000L, completedAt = 2500L),
            PurchaseItemEntity(id = 3, title = "Apple Cider", note = "Special bottle", isCompleted = true, createdAt = 3000L, completedAt = 3500L),
            PurchaseItemEntity(id = 4, title = "Coffee", note = "Arabica beans", isCompleted = false, createdAt = 4000L)
        )

        // Pending filter
        val pending = items.filter { !it.isCompleted }
        assertEquals(2, pending.size)
        assertTrue(pending.any { it.title == "Apples" })
        assertTrue(pending.any { it.title == "Coffee" })

        // Completed filter
        val completed = items.filter { it.isCompleted }
        assertEquals(2, completed.size)
        assertTrue(completed.any { it.title == "Bananas" })
        assertTrue(completed.any { it.title == "Apple Cider" })

        // Search query "apple"
        val query = "apple"
        val searched = items.filter { it.title.lowercase().contains(query) || it.note.lowercase().contains(query) }
        assertEquals(2, searched.size)
        assertTrue(searched.any { it.id == 1L })
        assertTrue(searched.any { it.id == 3L })

        // Search in Pending: "apple"
        val pendingApple = items.filter { !it.isCompleted && (it.title.lowercase().contains(query) || it.note.lowercase().contains(query)) }
        assertEquals(1, pendingApple.size)
        assertEquals(1L, pendingApple[0].id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `navigationToPurchasePlanner_updatesMoreSubDestinationAndBack`() = kotlinx.coroutines.test.runTest {
        val viewModel = FinanceViewModel(repository)
        val job = launch(kotlinx.coroutines.test.UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        // Initial destination must be NONE
        assertEquals(MoreSubDestination.NONE, viewModel.uiState.value.moreSubDestination)

        // Navigate to Purchase Planner (as triggered by the 3-dot overflow menu or More screen)
        viewModel.navigateToMoreSubDestination(MoreSubDestination.PURCHASE_PLANNER)
        assertEquals(MoreSubDestination.PURCHASE_PLANNER, viewModel.uiState.value.moreSubDestination)

        // Navigate back from Purchase Planner
        viewModel.navigateBackFromMoreSubDestination()
        assertEquals(MoreSubDestination.NONE, viewModel.uiState.value.moreSubDestination)

        job.cancel()
    }
}
