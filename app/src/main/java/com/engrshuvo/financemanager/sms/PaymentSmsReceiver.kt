package com.engrshuvo.financemanager.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.engrshuvo.financemanager.sms.parser.PaymentSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaymentSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Combine multi-part SMS if any, or process first segment
        val firstMsg = messages[0] ?: return
        val sender = firstMsg.displayOriginatingAddress ?: ""
        val timestamp = firstMsg.timestampMillis

        // Efficient string builder for multi-part messages
        val fullBody = if (messages.size == 1) {
            firstMsg.displayMessageBody ?: ""
        } else {
            messages.joinToString("") { it.displayMessageBody ?: "" }
        }

        // Ultra-lightweight pre-filter before allocating any coroutine or touching storage
        if (!PaymentSmsParser.isPotentialFinancialSms(sender, fullBody)) {
            return
        }

        // Use bounded goAsync execution on IO dispatcher
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PaymentSmsProcessor.processIncomingSms(
                    context = context.applicationContext,
                    sender = sender,
                    body = fullBody,
                    messageTimestamp = timestamp
                )
            } catch (_: Exception) {
                // Silently swallow in receiver to prevent crashing telephony broadcast thread
            } finally {
                pendingResult.finish()
            }
        }
    }
}
