package com.engrshuvo.financemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.engrshuvo.financemanager.data.model.FinancialGoalEntity
import com.engrshuvo.financemanager.data.model.GoalContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialGoalDao {
    @Query("SELECT * FROM financial_goals WHERE archivedAt IS NULL ORDER BY createdAt DESC")
    fun getAllGoalsFlow(): Flow<List<FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals WHERE status = 'ACTIVE' AND archivedAt IS NULL ORDER BY createdAt DESC")
    fun getActiveGoalsFlow(): Flow<List<FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun getArchivedGoalsFlow(): Flow<List<FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalByIdDirect(id: Long): FinancialGoalEntity?

    @Query("SELECT * FROM financial_goals WHERE id = :id LIMIT 1")
    fun getGoalByIdFlow(id: Long): Flow<FinancialGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: FinancialGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: FinancialGoalEntity)

    @Query("UPDATE financial_goals SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveGoal(id: Long, archivedAt: Long)

    @Query("UPDATE financial_goals SET archivedAt = NULL WHERE id = :id")
    suspend fun restoreGoal(id: Long)

    @Query("DELETE FROM financial_goals WHERE id = :id")
    suspend fun permanentlyDeleteGoalById(id: Long)

    @Query("DELETE FROM financial_goals WHERE archivedAt IS NOT NULL AND archivedAt <= :cutoffTime")
    suspend fun purgeExpiredGoals(cutoffTime: Long): Int

    @Query("SELECT * FROM financial_goals ORDER BY id ASC")
    suspend fun getAllGoalsForBackup(): List<FinancialGoalEntity>

    @Query("DELETE FROM financial_goals")
    suspend fun clearAllGoals()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoals(goals: List<FinancialGoalEntity>)
}

@Dao
interface GoalContributionDao {
    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId AND archivedAt IS NULL ORDER BY contributionDate DESC")
    fun getContributionsForGoalFlow(goalId: Long): Flow<List<GoalContributionEntity>>

    @Query("SELECT * FROM goal_contributions WHERE archivedAt IS NULL ORDER BY contributionDate DESC")
    fun getAllContributionsFlow(): Flow<List<GoalContributionEntity>>

    @Query("SELECT * FROM goal_contributions WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun getArchivedContributionsFlow(): Flow<List<GoalContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: GoalContributionEntity): Long

    @Update
    suspend fun updateContribution(contribution: GoalContributionEntity)

    @Query("DELETE FROM goal_contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long)

    @Query("UPDATE goal_contributions SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveContribution(id: Long, archivedAt: Long)

    @Query("UPDATE goal_contributions SET archivedAt = NULL WHERE id = :id")
    suspend fun restoreContribution(id: Long)

    @Query("UPDATE goal_contributions SET archivedAt = :archivedAt WHERE goalId = :goalId")
    suspend fun archiveContributionsForGoal(goalId: Long, archivedAt: Long)

    @Query("UPDATE goal_contributions SET archivedAt = NULL WHERE goalId = :goalId")
    suspend fun restoreContributionsForGoal(goalId: Long)

    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    suspend fun permanentlyDeleteContributionsForGoal(goalId: Long)

    @Query("DELETE FROM goal_contributions WHERE archivedAt IS NOT NULL AND archivedAt <= :cutoffTime")
    suspend fun purgeExpiredContributions(cutoffTime: Long): Int

    @Query("SELECT * FROM goal_contributions ORDER BY id ASC")
    suspend fun getAllContributionsForBackup(): List<GoalContributionEntity>

    @Query("DELETE FROM goal_contributions")
    suspend fun clearAllContributions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContributions(contributions: List<GoalContributionEntity>)
}
