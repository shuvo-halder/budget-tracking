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
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetSettingEntity::class,
        LoanEntity::class,
        LoanRepaymentEntity::class,
        BudgetAllocationEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetSettingDao(): BudgetSettingDao
    abstract fun loanDao(): LoanDao
    abstract fun budgetAllocationDao(): BudgetAllocationDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_manager_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
