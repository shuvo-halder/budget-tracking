package com.engrshuvo.financemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.engrshuvo.financemanager.data.local.AppDatabase
import com.engrshuvo.financemanager.data.model.TransactionType
import com.engrshuvo.financemanager.data.repository.FinanceRepository
import com.engrshuvo.financemanager.notification.FinanceNotificationManager
import com.engrshuvo.financemanager.notification.NotificationPreferencesRepository
import com.engrshuvo.financemanager.notification.NotificationScheduler
import com.engrshuvo.financemanager.ui.screens.MainFinanceScreen
import com.engrshuvo.financemanager.ui.state.FinanceTab
import com.engrshuvo.financemanager.ui.theme.FinanceManagerTheme
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModel
import com.engrshuvo.financemanager.ui.viewmodel.FinanceViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            budgetSettingDao = database.budgetSettingDao(),
            loanDao = database.loanDao(),
            budgetAllocationDao = database.budgetAllocationDao(),
            financialGoalDao = database.financialGoalDao(),
            goalContributionDao = database.goalContributionDao(),
            database = database
        )
        val notificationRepo = NotificationPreferencesRepository(applicationContext)
        FinanceViewModelFactory(repository, notificationRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FinanceNotificationManager.createNotificationChannels(applicationContext)

        val notificationRepo = NotificationPreferencesRepository(applicationContext)
        lifecycleScope.launch {
            val prefs = notificationRepo.preferencesFlow.first()
            NotificationScheduler.syncAllWork(applicationContext, prefs)
        }

        handleNotificationIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val destination = intent?.getStringExtra(FinanceNotificationManager.EXTRA_DESTINATION) ?: return
        when (destination) {
            FinanceNotificationManager.DESTINATION_ADD_EXPENSE -> {
                viewModel.setActiveTab(FinanceTab.DASHBOARD)
                viewModel.openAddTransactionSheet(TransactionType.EXPENSE)
            }
            FinanceNotificationManager.DESTINATION_DASHBOARD -> {
                viewModel.setActiveTab(FinanceTab.DASHBOARD)
            }
        }
    }
}
