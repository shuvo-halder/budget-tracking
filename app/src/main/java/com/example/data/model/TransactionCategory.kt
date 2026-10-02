package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TransactionCategory(
    val id: String,
    val name: String,
    val type: TransactionType,
    val icon: ImageVector,
    val color: Color
)

object CategoryCatalog {
    val expenseCategories: List<TransactionCategory> = listOf(
        TransactionCategory(
            id = "food",
            name = "Food & Dining",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.Fastfood,
            color = Color(0xFFF59E0B)
        ),
        TransactionCategory(
            id = "transport",
            name = "Transport",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.DirectionsBus,
            color = Color(0xFF3B82F6)
        ),
        TransactionCategory(
            id = "groceries",
            name = "Groceries",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF10B981)
        ),
        TransactionCategory(
            id = "bills",
            name = "Bills & Utilities",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.Bolt,
            color = Color(0xFFEC4899)
        ),
        TransactionCategory(
            id = "housing",
            name = "Housing & Rent",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.Home,
            color = Color(0xFF8B5CF6)
        ),
        TransactionCategory(
            id = "shopping",
            name = "Shopping",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.Store,
            color = Color(0xFF06B6D4)
        ),
        TransactionCategory(
            id = "entertainment",
            name = "Entertainment",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.Movie,
            color = Color(0xFFF43F5E)
        ),
        TransactionCategory(
            id = "health",
            name = "Health & Medical",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.LocalHospital,
            color = Color(0xFF14B8A6)
        ),
        TransactionCategory(
            id = "education",
            name = "Education",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.School,
            color = Color(0xFF6366F1)
        ),
        TransactionCategory(
            id = "personal_care",
            name = "Personal Care",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.Spa,
            color = Color(0xFFD946EF)
        ),
        TransactionCategory(
            id = "other_expense",
            name = "Other Expense",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.MoreHoriz,
            color = Color(0xFF64748B)
        )
    )

    val incomeCategories: List<TransactionCategory> = listOf(
        TransactionCategory(
            id = "salary",
            name = "Salary",
            type = TransactionType.INCOME,
            icon = Icons.Default.Work,
            color = Color(0xFF10B981)
        ),
        TransactionCategory(
            id = "freelance",
            name = "Freelance / Projects",
            type = TransactionType.INCOME,
            icon = Icons.Default.LaptopMac,
            color = Color(0xFF3B82F6)
        ),
        TransactionCategory(
            id = "business",
            name = "Business",
            type = TransactionType.INCOME,
            icon = Icons.Default.Store,
            color = Color(0xFFF59E0B)
        ),
        TransactionCategory(
            id = "investment",
            name = "Investment Return",
            type = TransactionType.INCOME,
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF8B5CF6)
        ),
        TransactionCategory(
            id = "gift",
            name = "Gifts & Allowance",
            type = TransactionType.INCOME,
            icon = Icons.Default.CardGiftcard,
            color = Color(0xFFEC4899)
        ),
        TransactionCategory(
            id = "bank_interest",
            name = "Interest & Profit",
            type = TransactionType.INCOME,
            icon = Icons.Default.AccountBalance,
            color = Color(0xFF14B8A6)
        ),
        TransactionCategory(
            id = "other_income",
            name = "Other Income",
            type = TransactionType.INCOME,
            icon = Icons.Default.AttachMoney,
            color = Color(0xFF64748B)
        )
    )

    fun getAllCategories(): List<TransactionCategory> = expenseCategories + incomeCategories

    fun getCategoryById(id: String): TransactionCategory {
        return getAllCategories().find { it.id == id }
            ?: TransactionCategory(
                id = id,
                name = id.replaceFirstChar { it.uppercase() },
                type = TransactionType.EXPENSE,
                icon = Icons.Default.ReceiptLong,
                color = Color(0xFF64748B)
            )
    }
}
