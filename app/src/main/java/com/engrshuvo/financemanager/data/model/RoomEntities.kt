package com.engrshuvo.financemanager.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "transactions",
    indices = [Index("archivedAt"), Index("loanId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val loanId: Long? = null,
    val archivedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "loans",
    indices = [Index("archivedAt")]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: LoanType,
    val personName: String,
    val phoneNumber: String = "",
    val initialAmount: Double,
    val remainingAmount: Double,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val startDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val note: String = "",
    val archivedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "loan_repayments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("loanId"), Index("archivedAt")]
)
data class LoanRepaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)

@Immutable
@Entity(tableName = "budget_settings")
data class BudgetSettingEntity(
    @PrimaryKey
    val settingKey: String = KEY_MONTHLY_BUDGET,
    val amountLimit: Double = 30000.0,
    val currencyCode: String = "BDT"
) {
    companion object {
        const val KEY_MONTHLY_BUDGET = "monthly_budget_limit"
        const val KEY_DAILY_BUDGET = "daily_budget_limit"
    }
}

@Immutable
@Entity(
    tableName = "budget_allocations",
    primaryKeys = ["monthKey", "categoryId"],
    indices = [Index("monthKey")]
)
data class BudgetAllocationEntity(
    val monthKey: String, // Normalized "yyyy-MM", e.g. "2026-10"
    val categoryId: String,
    val categoryName: String,
    val allocatedAmount: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "financial_goals",
    indices = [Index("archivedAt"), Index("status")]
)
data class FinancialGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: GoalCategory = GoalCategory.OTHER,
    val targetAmount: Double,
    val initialSavedAmount: Double = 0.0,
    val targetDate: Long? = null,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val targetMonthlyContribution: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "goal_contributions",
    foreignKeys = [
        ForeignKey(
            entity = FinancialGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId"), Index("archivedAt"), Index("contributionDate")]
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val contributionDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)

