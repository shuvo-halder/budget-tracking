package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import com.example.data.model.TransactionType

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
}
