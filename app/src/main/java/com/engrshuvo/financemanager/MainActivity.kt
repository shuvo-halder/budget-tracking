package com.engrshuvo.financemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.ui.screens.MainFinanceScreen
import com.engrshuvo.financemanager.ui.theme.FinanceManagerTheme
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModel
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            budgetSettingDao = database.budgetSettingDao(),
            loanDao = database.loanDao(),
            budgetAllocationDao = database.budgetAllocationDao()
        )
        FinanceViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainFinanceScreen(viewModel = viewModel)
                }
            }
        }
    }
}
