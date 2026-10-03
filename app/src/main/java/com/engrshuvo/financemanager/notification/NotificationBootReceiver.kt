package com.engrshuvo.financemanager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.TIME_SET"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FinanceNotificationManager.createNotificationChannels(context)
                    val prefsRepo = NotificationPreferencesRepository(context)
                    val prefs = prefsRepo.preferencesFlow.first()
                    NotificationScheduler.syncAllWork(context, prefs)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
