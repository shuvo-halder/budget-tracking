package com.engrshuvo.financemanager.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class TransactionCategory(
    val id: String,
    val name: String,
    val type: TransactionType,
    val icon: ImageVector,
    val color: Color
)

object CategoryCatalog {
    val expenseCategories = listOf(
        TransactionCategory("food", "Food & Dining", TransactionType.EXPENSE, Icons.Default.Fastfood, Color(0xFFF97316)),
        TransactionCategory("grocery", "Groceries", TransactionType.EXPENSE, Icons.Default.ShoppingCart, Color(0xFF10B981)),
        TransactionCategory("transport", "Transportation", TransactionType.EXPENSE, Icons.Default.DirectionsBus, Color(0xFF3B82F6)),
        TransactionCategory("shopping", "Shopping", TransactionType.EXPENSE, Icons.Default.LocalMall, Color(0xFFEC4899)),
        TransactionCategory("bills", "Bills & Utilities", TransactionType.EXPENSE, Icons.Default.Receipt, Color(0xFF8B5CF6)),
        TransactionCategory("housing", "Rent & Housing", TransactionType.EXPENSE, Icons.Default.Home, Color(0xFF06B6D4)),
        TransactionCategory("health", "Health & Medical", TransactionType.EXPENSE, Icons.Default.HealthAndSafety, Color(0xFFEF4444)),
        TransactionCategory("entertainment", "Entertainment", TransactionType.EXPENSE, Icons.Default.Movie, Color(0xFFA855F7)),
        TransactionCategory("education", "Education", TransactionType.EXPENSE, Icons.Default.School, Color(0xFFEAB308)),
        TransactionCategory("fitness", "Fitness & Sports", TransactionType.EXPENSE, Icons.Default.FitnessCenter, Color(0xFF14B8A6)),
        TransactionCategory("other_expense", "Other Expense", TransactionType.EXPENSE, Icons.Default.Payments, Color(0xFF64748B))
    )

    val incomeCategories = listOf(
        TransactionCategory("salary", "Salary", TransactionType.INCOME, Icons.Default.Payments, Color(0xFF10B981)),
        TransactionCategory("business", "Business & Sales", TransactionType.INCOME, Icons.Default.Work, Color(0xFF3B82F6)),
        TransactionCategory("freelance", "Freelance", TransactionType.INCOME, Icons.Default.Commute, Color(0xFF8B5CF6)),
        TransactionCategory("investment", "Investments", TransactionType.INCOME, Icons.Default.AccountBalance, Color(0xFF06B6D4)),
        TransactionCategory("gift", "Gift & Bonus", TransactionType.INCOME, Icons.Default.CardGiftcard, Color(0xFFEC4899)),
        TransactionCategory("other_income", "Other Income", TransactionType.INCOME, Icons.Default.Payments, Color(0xFF64748B))
    )

    fun getAllCategories(): List<TransactionCategory> = expenseCategories + incomeCategories

    fun getCategoryById(id: String): TransactionCategory {
        return getAllCategories().find { it.id == id }
            ?: when {
                id.startsWith("loan") -> TransactionCategory(
                    id = id,
                    name = "Loan Activity",
                    type = TransactionType.LOAN,
                    icon = Icons.Default.Handshake,
                    color = Color(0xFF3B82F6)
                )
                else -> TransactionCategory(
                    id = id,
                    name = "General",
                    type = TransactionType.EXPENSE,
                    icon = Icons.Default.Payments,
                    color = Color(0xFF64748B)
                )
            }
    }
}
