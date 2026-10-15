package com.engrshuvo.financemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.engrshuvo.financemanager.data.model.PurchaseItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseItemDao {

    @Query("SELECT * FROM purchase_items ORDER BY createdAt DESC")
    fun getAllItemsFlow(): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getPendingItemsFlow(): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE isCompleted = 1 ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedItemsFlow(): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): PurchaseItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PurchaseItemEntity): Long

    @Update
    suspend fun updateItem(item: PurchaseItemEntity): Int

    @Query("UPDATE purchase_items SET isCompleted = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE purchase_items SET isCompleted = 0, completedAt = NULL WHERE id = :id")
    suspend fun restoreToPending(id: Long): Int

    @Query("DELETE FROM purchase_items WHERE id = :id")
    suspend fun deleteItemById(id: Long): Int

    @Delete
    suspend fun deleteItem(item: PurchaseItemEntity): Int

    @Query("SELECT * FROM purchase_items ORDER BY id ASC")
    suspend fun getAllItemsDirect(): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PurchaseItemEntity>)

    @Query("DELETE FROM purchase_items")
    suspend fun clearAll()
}
