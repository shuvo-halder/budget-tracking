package com.engrshuvo.financemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.engrshuvo.financemanager.data.model.BudgetSettingEntity
import com.engrshuvo.financemanager.data.model.LoanEntity
import com.engrshuvo.financemanager.data.model.LoanRepaymentEntity
import com.engrshuvo.financemanager.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetSettingEntity::class,
        LoanEntity::class,
        LoanRepaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetSettingDao(): BudgetSettingDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_manager_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
