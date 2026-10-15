package com.engrshuvo.financemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.engrshuvo.financemanager.data.model.BudgetAllocationEntity
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.PurchaseItemEntity
import com.engrshuvo.financemanager.data.model.SmsImportedRecordEntity
import com.engrshuvo.financemanager.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetSettingEntity::class,
        LoanEntity::class,
        LoanRepaymentEntity::class,
        BudgetAllocationEntity::class,
        FinancialGoalEntity::class,
        GoalContributionEntity::class,
        PurchaseItemEntity::class,
        SmsImportedRecordEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetSettingDao(): BudgetSettingDao
    abstract fun loanDao(): LoanDao
    abstract fun budgetAllocationDao(): BudgetAllocationDao
    abstract fun financialGoalDao(): FinancialGoalDao
    abstract fun goalContributionDao(): GoalContributionDao
    abstract fun purchaseItemDao(): PurchaseItemDao
    abstract fun smsImportedRecordDao(): SmsImportedRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_budget_allocations_monthKey` ON `budget_allocations` (`monthKey`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `loans` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `loan_repayments` ADD COLUMN `archivedAt` INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_archivedAt` ON `transactions` (`archivedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_archivedAt` ON `loans` (`archivedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_repayments_archivedAt` ON `loan_repayments` (`archivedAt`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `initialSavedAmount` REAL NOT NULL,
                        `targetDate` INTEGER,
                        `priority` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `targetMonthlyContribution` REAL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `archivedAt` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_goals_archivedAt` ON `financial_goals` (`archivedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_goals_status` ON `financial_goals` (`status`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goal_contributions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `contributionDate` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `archivedAt` INTEGER,
                        FOREIGN KEY(`goalId`) REFERENCES `financial_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_goalId` ON `goal_contributions` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_archivedAt` ON `goal_contributions` (`archivedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_contributionDate` ON `goal_contributions` (`contributionDate`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `purchase_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_items_isCompleted` ON `purchase_items` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_items_createdAt` ON `purchase_items` (`createdAt`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sms_imported_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `trxId` TEXT,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `fee` REAL NOT NULL,
                        `counterparty` TEXT NOT NULL,
                        `suggestedCategory` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `linkedTransactionId` INTEGER,
                        `fallbackFingerprint` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_imported_records_trxId` ON `sms_imported_records` (`trxId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_imported_records_timestamp` ON `sms_imported_records` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_imported_records_status` ON `sms_imported_records` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_imported_records_fallbackFingerprint` ON `sms_imported_records` (`fallbackFingerprint`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_manager_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
