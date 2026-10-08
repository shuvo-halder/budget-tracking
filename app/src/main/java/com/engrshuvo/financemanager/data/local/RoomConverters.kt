package com.engrshuvo.financemanager.data.local

import androidx.room.TypeConverter
import com.engrshuvo.financemanager.data.model.LoanStatus
import com.engrshuvo.financemanager.data.model.LoanType
import com.engrshuvo.financemanager.data.model.TransactionType

class RoomConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromLoanType(value: LoanType): String = value.name

    @TypeConverter
    fun toLoanType(value: String): LoanType {
        return try {
            LoanType.valueOf(value)
        } catch (e: Exception) {
            LoanType.LENT
        }
    }

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String = value.name

    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus {
        return try {
            LoanStatus.valueOf(value)
        } catch (e: Exception) {
            LoanStatus.ACTIVE
        }
    }

    @TypeConverter
    fun fromGoalCategory(value: com.engrshuvo.financemanager.data.model.GoalCategory): String = value.name

    @TypeConverter
    fun toGoalCategory(value: String): com.engrshuvo.financemanager.data.model.GoalCategory {
        return try {
            com.engrshuvo.financemanager.data.model.GoalCategory.valueOf(value)
        } catch (e: Exception) {
            com.engrshuvo.financemanager.data.model.GoalCategory.OTHER
        }
    }

    @TypeConverter
    fun fromGoalPriority(value: com.engrshuvo.financemanager.data.model.GoalPriority): String = value.name

    @TypeConverter
    fun toGoalPriority(value: String): com.engrshuvo.financemanager.data.model.GoalPriority {
        return try {
            com.engrshuvo.financemanager.data.model.GoalPriority.valueOf(value)
        } catch (e: Exception) {
            com.engrshuvo.financemanager.data.model.GoalPriority.MEDIUM
        }
    }

    @TypeConverter
    fun fromGoalStatus(value: com.engrshuvo.financemanager.data.model.GoalStatus): String = value.name

    @TypeConverter
    fun toGoalStatus(value: String): com.engrshuvo.financemanager.data.model.GoalStatus {
        return try {
            com.engrshuvo.financemanager.data.model.GoalStatus.valueOf(value)
        } catch (e: Exception) {
            com.engrshuvo.financemanager.data.model.GoalStatus.ACTIVE
        }
    }
}
