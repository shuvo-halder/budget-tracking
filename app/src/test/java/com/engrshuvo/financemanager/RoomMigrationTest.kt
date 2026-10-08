package com.engrshuvo.financemanager

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.engrshuvo.financemanager.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {

    @Test
    fun `migration from v1 to v2 creates budget_allocations table without data loss`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_v1_v2.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Version 1 Schema
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `transactions` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `type` TEXT NOT NULL,
                                `amount` REAL NOT NULL,
                                `categoryId` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `loanId` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `loans` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `type` TEXT NOT NULL,
                                `personName` TEXT NOT NULL,
                                `phoneNumber` TEXT NOT NULL,
                                `initialAmount` REAL NOT NULL,
                                `remainingAmount` REAL NOT NULL,
                                `status` TEXT NOT NULL,
                                `startDate` INTEGER NOT NULL,
                                `dueDate` INTEGER,
                                `note` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `loan_repayments` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `loanId` INTEGER NOT NULL,
                                `amount` REAL NOT NULL,
                                `note` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                FOREIGN KEY(`loanId`) REFERENCES `loans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `budget_settings` (
                                `settingKey` TEXT PRIMARY KEY NOT NULL,
                                `amountLimit` REAL NOT NULL,
                                `currencyCode` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = openHelper.writableDatabase

        // Insert v1 transaction
        db.execSQL(
            "INSERT INTO transactions (id, type, amount, categoryId, categoryName, note, timestamp) VALUES (1, 'INCOME', 50000.0, 'salary', 'Salary', 'Oct Pay', 1696118400000)"
        )

        // Run Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify budget_allocations table exists and works
        db.execSQL(
            "INSERT INTO budget_allocations (monthKey, categoryId, categoryName, allocatedAmount, updatedAt) VALUES ('2026-10', 'housing', 'Rent', 12000.0, 1696118400000)"
        )

        val cursor = db.query("SELECT allocatedAmount FROM budget_allocations WHERE monthKey = '2026-10' AND categoryId = 'housing'")
        assertTrue(cursor.moveToFirst())
        assertEquals(12000.0, cursor.getDouble(0), 0.001)
        cursor.close()

        // Verify original v1 record is preserved
        val txCursor = db.query("SELECT amount, categoryName FROM transactions WHERE id = 1")
        assertTrue(txCursor.moveToFirst())
        assertEquals(50000.0, txCursor.getDouble(0), 0.001)
        assertEquals("Salary", txCursor.getString(1))
        txCursor.close()

        db.close()
    }

    @Test
    fun `migration from v2 to v3 adds nullable archivedAt and preserves active status`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_v2_v3.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Version 2 Schema
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `transactions` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `type` TEXT NOT NULL,
                                `amount` REAL NOT NULL,
                                `categoryId` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `loanId` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `loans` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `type` TEXT NOT NULL,
                                `personName` TEXT NOT NULL,
                                `phoneNumber` TEXT NOT NULL,
                                `initialAmount` REAL NOT NULL,
                                `remainingAmount` REAL NOT NULL,
                                `status` TEXT NOT NULL,
                                `startDate` INTEGER NOT NULL,
                                `dueDate` INTEGER,
                                `note` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `loan_repayments` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `loanId` INTEGER NOT NULL,
                                `amount` REAL NOT NULL,
                                `note` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `budget_settings` (
                                `settingKey` TEXT PRIMARY KEY NOT NULL,
                                `amountLimit` REAL NOT NULL,
                                `currencyCode` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `budget_allocations` (
                                `monthKey` TEXT NOT NULL,
                                `categoryId` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `allocatedAmount` REAL NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`monthKey`, `categoryId`)
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = openHelper.writableDatabase

        // Insert pre-migration active records
        db.execSQL("INSERT INTO transactions (id, type, amount, categoryId, categoryName, note, timestamp) VALUES (10, 'EXPENSE', 1500.0, 'grocery', 'Groceries', 'Supermarket', 1696118400000)")
        db.execSQL("INSERT INTO loans (id, type, personName, phoneNumber, initialAmount, remainingAmount, status, startDate, note) VALUES (5, 'LENT', 'Hasan', '01711', 5000.0, 5000.0, 'ACTIVE', 1696118400000, '')")
        db.execSQL("INSERT INTO loan_repayments (id, loanId, amount, note, timestamp) VALUES (1, 5, 1000.0, 'First installment', 1696118400000)")

        // Run Migration 2 -> 3
        AppDatabase.MIGRATION_2_3.migrate(db)

        // Verify that archivedAt column exists and defaults to NULL for existing records (retaining active status)
        val txCursor = db.query("SELECT id, amount, archivedAt FROM transactions WHERE id = 10")
        assertTrue(txCursor.moveToFirst())
        assertEquals(10L, txCursor.getLong(0))
        assertEquals(1500.0, txCursor.getDouble(1), 0.001)
        assertTrue(txCursor.isNull(2)) // Must be null!
        txCursor.close()

        val loanCursor = db.query("SELECT id, personName, archivedAt FROM loans WHERE id = 5")
        assertTrue(loanCursor.moveToFirst())
        assertEquals(5L, loanCursor.getLong(0))
        assertEquals("Hasan", loanCursor.getString(1))
        assertTrue(loanCursor.isNull(2)) // Must be null!
        loanCursor.close()

        val repCursor = db.query("SELECT id, amount, archivedAt FROM loan_repayments WHERE id = 1")
        assertTrue(repCursor.moveToFirst())
        assertEquals(1L, repCursor.getLong(0))
        assertEquals(1000.0, repCursor.getDouble(1), 0.001)
        assertTrue(repCursor.isNull(2)) // Must be null!
        repCursor.close()

        // Verify that archiving sets archivedAt and active query excludes it
        db.execSQL("UPDATE transactions SET archivedAt = 1696200000000 WHERE id = 10")
        val activeCursor = db.query("SELECT COUNT(*) FROM transactions WHERE archivedAt IS NULL")
        assertTrue(activeCursor.moveToFirst())
        assertEquals(0, activeCursor.getInt(0))
        activeCursor.close()

        val archivedCursor = db.query("SELECT COUNT(*) FROM transactions WHERE archivedAt IS NOT NULL")
        assertTrue(archivedCursor.moveToFirst())
        assertEquals(1, archivedCursor.getInt(0))
        archivedCursor.close()

        db.close()
    }

    @Test
    fun `migration from v3 to v4 creates financial_goals and goal_contributions tables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_v3_v4.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Version 3 Schema
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `transactions` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `type` TEXT NOT NULL,
                                `amount` REAL NOT NULL,
                                `categoryId` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `loanId` INTEGER,
                                `archivedAt` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `loans` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `type` TEXT NOT NULL,
                                `personName` TEXT NOT NULL,
                                `phoneNumber` TEXT NOT NULL,
                                `initialAmount` REAL NOT NULL,
                                `remainingAmount` REAL NOT NULL,
                                `status` TEXT NOT NULL,
                                `startDate` INTEGER NOT NULL,
                                `dueDate` INTEGER,
                                `note` TEXT NOT NULL,
                                `archivedAt` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `loan_repayments` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `loanId` INTEGER NOT NULL,
                                `amount` REAL NOT NULL,
                                `note` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `archivedAt` INTEGER,
                                FOREIGN KEY(`loanId`) REFERENCES `loans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `budget_settings` (
                                `settingKey` TEXT PRIMARY KEY NOT NULL,
                                `amountLimit` REAL NOT NULL,
                                `currencyCode` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `budget_allocations` (
                                `monthKey` TEXT NOT NULL,
                                `categoryId` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `allocatedAmount` REAL NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`monthKey`, `categoryId`)
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = openHelper.writableDatabase

        // Run Migration 3 -> 4
        AppDatabase.MIGRATION_3_4.migrate(db)

        // Insert into financial_goals
        db.execSQL(
            """
            INSERT INTO financial_goals (
                id, name, category, targetAmount, initialSavedAmount, targetDate, priority, status, targetMonthlyContribution, createdAt, updatedAt, archivedAt
            ) VALUES (
                1, 'New Laptop', 'COMPUTER', 80000.0, 20000.0, 1735689600000, 'HIGH', 'ACTIVE', 10000.0, 1728390000000, 1728390000000, NULL
            )
            """.trimIndent()
        )

        // Insert into goal_contributions
        db.execSQL(
            """
            INSERT INTO goal_contributions (
                id, goalId, amount, contributionDate, note, createdAt, archivedAt
            ) VALUES (
                1, 1, 5000.0, 1728395000000, 'October saving deposit', 1728395000000, NULL
            )
            """.trimIndent()
        )

        val goalCursor = db.query("SELECT name, targetAmount, initialSavedAmount FROM financial_goals WHERE id = 1")
        assertTrue(goalCursor.moveToFirst())
        assertEquals("New Laptop", goalCursor.getString(0))
        assertEquals(80000.0, goalCursor.getDouble(1), 0.001)
        assertEquals(20000.0, goalCursor.getDouble(2), 0.001)
        goalCursor.close()

        val contCursor = db.query("SELECT amount, note FROM goal_contributions WHERE goalId = 1")
        assertTrue(contCursor.moveToFirst())
        assertEquals(5000.0, contCursor.getDouble(0), 0.001)
        assertEquals("October saving deposit", contCursor.getString(1))
        contCursor.close()

        db.close()
    }
}
