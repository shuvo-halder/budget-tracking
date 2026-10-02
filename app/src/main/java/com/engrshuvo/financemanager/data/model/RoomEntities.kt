package com.engrshuvo.financemanager.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val loanId: Long? = null
)

@Immutable
@Entity(tableName = "loans")
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
    val note: String = ""
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
    indices = [Index("loanId")]
)
data class LoanRepaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
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
    }
}
