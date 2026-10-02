package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.TransactionType

class RoomConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
    }
}
