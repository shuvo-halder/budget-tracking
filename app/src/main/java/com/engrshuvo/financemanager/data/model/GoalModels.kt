package com.engrshuvo.financemanager.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class GoalCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    VEHICLE("Vehicle", Icons.Default.DirectionsCar, Color(0xFF2563EB)),
    COMPUTER("Computer", Icons.Default.Computer, Color(0xFF7C3AED)),
    HOME("Home", Icons.Default.Home, Color(0xFF059669)),
    MARRIAGE("Marriage", Icons.Default.Loyalty, Color(0xFFEC4899)),
    TRAVEL("Travel", Icons.Default.Flight, Color(0xFF06B6D4)),
    EDUCATION("Education", Icons.Default.School, Color(0xFFEAB308)),
    EMERGENCY("Emergency Fund", Icons.Default.HealthAndSafety, Color(0xFFDC2626)),
    OTHER("Custom Goal", Icons.Default.Savings, Color(0xFF64748B))
}

enum class GoalPriority(val displayName: String, val color: Color) {
    HIGH("High", Color(0xFFEF4444)),
    MEDIUM("Medium", Color(0xFFF59E0B)),
    LOW("Low", Color(0xFF10B981))
}

enum class GoalStatus(val displayName: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

@Immutable
data class FinancialGoalUiModel(
    val goal: FinancialGoalEntity,
    val totalSaved: Double,
    val remainingAmount: Double,
    val progress: Float,
    val progressPercent: Double,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val remainingMonths: Int?,
    val requiredMonthlySaving: Double?,
    val thisMonthSaved: Double,
    val contributions: List<GoalContributionEntity> = emptyList()
)
