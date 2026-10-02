package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.repository.BudgetRepository
import com.example.ui.screens.MainBudgetScreen
import com.example.ui.theme.DailyBudgetTheme
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.BudgetViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BudgetRepository(
            transactionDao = database.transactionDao(),
            budgetSettingDao = database.budgetSettingDao()
        )
        BudgetViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyBudgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainBudgetScreen(viewModel = viewModel)
                }
            }
        }
    }
}
