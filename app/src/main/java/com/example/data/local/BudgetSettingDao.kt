package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BudgetSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetSettingDao {
    @Query("SELECT * FROM budget_settings WHERE settingKey = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<BudgetSettingEntity?>

    @Query("SELECT * FROM budget_settings WHERE settingKey = :key LIMIT 1")
    suspend fun getSettingDirect(key: String): BudgetSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: BudgetSettingEntity)
}
