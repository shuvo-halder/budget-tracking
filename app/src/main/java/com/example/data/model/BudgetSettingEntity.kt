package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_settings")
data class BudgetSettingEntity(
    @PrimaryKey
    val settingKey: String = KEY_MONTHLY_BUDGET,
    val amountLimit: Double = 25000.0,
    val currencyCode: String = "BDT"
) {
    companion object {
        const val KEY_MONTHLY_BUDGET = "monthly_budget_target"
    }
}
