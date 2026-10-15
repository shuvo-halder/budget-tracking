package com.engrshuvo.financemanager.sms.parser

import com.engrshuvo.financemanager.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedPaymentSms(
    val provider: String,
    val sender: String,
    val type: TransactionType,
    val amount: Double,
    val fee: Double = 0.0,
    val trxId: String? = null,
    val counterparty: String = "",
    val suggestedCategory: String = "other_expense",
    val timestamp: Long = System.currentTimeMillis(),
    val isConfident: Boolean = true,
    val note: String = "",
    val isCardBillPayment: Boolean = false
)

object PaymentSmsParser {

    private val OTP_PROMO_PATTERN = Pattern.compile(
        "(?i)\\b(otp|verification code|security code|is your code|do not share|dial \\*|cashback offer|recharge offer|win up to|bonus offer|dial \\*247#|dial \\*167#|mini statement|current balance is|your pin is)\\b"
    )

    private val AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:tk\\.?|bdt\\.?)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)"
    )

    private val FEE_PATTERN = Pattern.compile(
        "(?i)fee\\s*:?\\s*(?:tk\\.?|bdt\\.?)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)"
    )

    private val TRX_ID_PATTERN = Pattern.compile(
        "(?i)(?:trxid|trx\\s*id|txnid|txn\\s*id|txid|ref|trx\\s*ref)[:\\s]+([A-Za-z0-9]+)"
    )

    private val FAILED_TRANSACTION_PATTERN = Pattern.compile(
        "(?i)\\b(failed|unsuccessful|declined|cancelled|canceled|could not be processed|insufficient balance|payment failed)\\b"
    )

    fun isSupportedSender(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        val b = body.lowercase()
        return s.contains("bkash") || b.contains("bkash") ||
                s.contains("nagad") || s.contains("16167") || b.contains("nagad") ||
                s.contains("rocket") || s.contains("16216") || b.contains("rocket") ||
                s.contains("upay") || s.contains("16268") || b.contains("upay") ||
                s.contains("tap") || s.contains("16733") || b.contains("trust axiata") ||
                s.contains("mcash") || s.contains("16259") || b.contains("mcash") ||
                s.contains("ok wallet") || s.contains("okwallet") || s.contains("16269") || b.contains("ok wallet") ||
                s.contains("mycash") || b.contains("mycash") ||
                s.contains("surecash") || b.contains("surecash") ||
                s.contains("bank") || s.contains("city") || s.contains("ebl") ||
                s.contains("scb") || s.contains("dbbl") || s.contains("card") ||
                s.contains("brac") || s.contains("ibbl") || s.contains("prime") ||
                s.contains("dhaka") || s.contains("ucb") || s.contains("islami") ||
                s.contains("standard chartered") || s.contains("eastern") ||
                b.contains("credit card") || b.contains("debit card") || b.contains("bank a/c")
    }

    fun isPotentialFinancialSms(sender: String, body: String): Boolean {
        if (body.isBlank()) return false
        if (OTP_PROMO_PATTERN.matcher(body).find()) return false
        if (FAILED_TRANSACTION_PATTERN.matcher(body).find()) return false
        if (!isSupportedSender(sender, body)) return false

        val containsCurrency = body.contains("Tk", ignoreCase = true) ||
                body.contains("BDT", ignoreCase = true)

        val containsFinancialKeyword = body.contains("paid", ignoreCase = true) ||
                body.contains("payment", ignoreCase = true) ||
                body.contains("debited", ignoreCase = true) ||
                body.contains("credited", ignoreCase = true) ||
                body.contains("received", ignoreCase = true) ||
                body.contains("cash out", ignoreCase = true) ||
                body.contains("cash in", ignoreCase = true) ||
                body.contains("send money", ignoreCase = true) ||
                body.contains("recharge", ignoreCase = true) ||
                body.contains("bill", ignoreCase = true) ||
                body.contains("purchased", ignoreCase = true) ||
                body.contains("withdrawn", ignoreCase = true) ||
                body.contains("transferred", ignoreCase = true) ||
                body.contains("refund", ignoreCase = true) ||
                body.contains("reversed", ignoreCase = true)

        return containsCurrency && containsFinancialKeyword
    }

    fun parse(sender: String, body: String, messageTimestamp: Long = System.currentTimeMillis()): ParsedPaymentSms? {
        if (!isPotentialFinancialSms(sender, body)) return null

        val cleanBody = body.trim()
        val s = sender.trim().lowercase()

        return when {
            s.contains("bkash") || cleanBody.contains("bKash", ignoreCase = true) ->
                parseBkash(sender, cleanBody, messageTimestamp)

            s.contains("nagad") || s.contains("16167") || cleanBody.contains("Nagad", ignoreCase = true) ->
                parseNagad(sender, cleanBody, messageTimestamp)

            s.contains("rocket") || s.contains("16216") || cleanBody.contains("Rocket", ignoreCase = true) ->
                parseRocket(sender, cleanBody, messageTimestamp)

            s.contains("upay") || s.contains("16268") || cleanBody.contains("upay", ignoreCase = true) ->
                parseUpay(sender, cleanBody, messageTimestamp)

            s.contains("tap") || s.contains("16733") || cleanBody.contains("Trust Axiata", ignoreCase = true) ->
                parseTap(sender, cleanBody, messageTimestamp)

            s.contains("mcash") || s.contains("16259") || cleanBody.contains("mCash", ignoreCase = true) ->
                parseMcash(sender, cleanBody, messageTimestamp)

            s.contains("ok wallet") || s.contains("okwallet") || s.contains("16269") || cleanBody.contains("OK Wallet", ignoreCase = true) ->
                parseOkWallet(sender, cleanBody, messageTimestamp)

            s.contains("mycash") || cleanBody.contains("MyCash", ignoreCase = true) ->
                parseMyCash(sender, cleanBody, messageTimestamp)

            s.contains("surecash") || cleanBody.contains("SureCash", ignoreCase = true) ->
                parseSureCash(sender, cleanBody, messageTimestamp)

            else ->
                parseBankOrCard(sender, cleanBody, messageTimestamp)
        }
    }

    private fun extractAmount(text: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "") ?: return null
            val value = raw.toDoubleOrNull() ?: return null
            if (value > 0.0) return value
        }
        return null
    }

    private fun extractFee(text: String): Double {
        val matcher = FEE_PATTERN.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "") ?: return 0.0
            return raw.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractTrxId(text: String): String? {
        val matcher = TRX_ID_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun parseBkash(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)

        val isReceived = body.contains("received", ignoreCase = true) || body.contains("Cash In", ignoreCase = true)
        val type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE

        val counterparty = when {
            body.contains(" to ", ignoreCase = true) -> {
                val afterTo = body.substringAfter(" to ", "").substringBefore(" successful").substringBefore(".").trim()
                afterTo.take(40)
            }
            body.contains(" from ", ignoreCase = true) -> {
                val afterFrom = body.substringAfter(" from ", "").substringBefore(" successful").substringBefore(".").trim()
                afterFrom.take(40)
            }
            else -> ""
        }

        val suggestedCategory = when {
            isReceived -> "other_income"
            body.contains("Pay Bill", ignoreCase = true) || body.contains("Recharge", ignoreCase = true) -> "bills"
            body.contains("Payment", ignoreCase = true) -> "shopping"
            else -> "other_expense"
        }

        val note = when {
            body.contains("Payment", ignoreCase = true) -> "bKash Payment${if (counterparty.isNotBlank()) " to $counterparty" else ""}"
            body.contains("Send Money", ignoreCase = true) -> "bKash Send Money${if (counterparty.isNotBlank()) " to $counterparty" else ""}"
            body.contains("Cash Out", ignoreCase = true) -> "bKash Cash Out${if (counterparty.isNotBlank()) " from $counterparty" else ""}"
            body.contains("Mobile Recharge", ignoreCase = true) -> "bKash Mobile Recharge"
            body.contains("Pay Bill", ignoreCase = true) -> "bKash Utility Bill Payment"
            isReceived -> "bKash Received Money${if (counterparty.isNotBlank()) " from $counterparty" else ""}"
            else -> "bKash Transaction"
        }

        return ParsedPaymentSms(
            provider = "bKash",
            sender = sender,
            type = type,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory,
            timestamp = timestamp,
            isConfident = trxId != null,
            note = note
        )
    }

    private fun parseNagad(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)

        val isReceived = body.contains("received", ignoreCase = true) || body.contains("Cash In", ignoreCase = true)
        val type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE

        val counterparty = when {
            body.contains(" to ", ignoreCase = true) -> {
                body.substringAfter(" to ", "").substringBefore(" successful").substringBefore(".").trim().take(40)
            }
            body.contains(" from ", ignoreCase = true) -> {
                body.substringAfter(" from ", "").substringBefore(" successful").substringBefore(".").trim().take(40)
            }
            else -> ""
        }

        val suggestedCategory = when {
            isReceived -> "other_income"
            body.contains("Bill", ignoreCase = true) || body.contains("Recharge", ignoreCase = true) -> "bills"
            body.contains("Payment", ignoreCase = true) -> "shopping"
            else -> "other_expense"
        }

        val note = when {
            body.contains("Payment", ignoreCase = true) -> "Nagad Payment${if (counterparty.isNotBlank()) " to $counterparty" else ""}"
            body.contains("Send Money", ignoreCase = true) -> "Nagad Send Money"
            body.contains("Cash Out", ignoreCase = true) -> "Nagad Cash Out"
            body.contains("Mobile Recharge", ignoreCase = true) -> "Nagad Mobile Recharge"
            isReceived -> "Nagad Received Funds"
            else -> "Nagad Transaction"
        }

        return ParsedPaymentSms(
            provider = "Nagad",
            sender = sender,
            type = type,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory,
            timestamp = timestamp,
            isConfident = trxId != null,
            note = note
        )
    }

    private fun parseRocket(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)

        val isReceived = body.contains("received from", ignoreCase = true) || body.contains("Cash In", ignoreCase = true)
        val type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE

        val counterparty = when {
            body.contains("paid to ", ignoreCase = true) -> {
                body.substringAfter("paid to ", "").substringBefore(" for").substringBefore(".").trim().take(40)
            }
            body.contains("received from ", ignoreCase = true) -> {
                body.substringAfter("received from ", "").substringBefore(".").trim().take(40)
            }
            body.contains("transferred to ", ignoreCase = true) -> {
                body.substringAfter("transferred to ", "").substringBefore(".").trim().take(40)
            }
            else -> ""
        }

        val suggestedCategory = when {
            isReceived -> "other_income"
            body.contains("Merchant Pay", ignoreCase = true) -> "shopping"
            else -> "other_expense"
        }

        return ParsedPaymentSms(
            provider = "Rocket",
            sender = sender,
            type = type,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory,
            timestamp = timestamp,
            isConfident = trxId != null,
            note = "Rocket ${if (isReceived) "Received Money" else "Payment"}"
        )
    }

    private fun parseUpay(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)

        val isReceived = body.contains("Received", ignoreCase = true)
        val type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE

        return ParsedPaymentSms(
            provider = "upay",
            sender = sender,
            type = type,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = "",
            suggestedCategory = if (isReceived) "other_income" else "other_expense",
            timestamp = timestamp,
            isConfident = trxId != null,
            note = "upay ${if (isReceived) "Received Funds" else "Payment"}"
        )
    }

    private fun parseTap(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)
        val isReceived = body.contains("Received", ignoreCase = true)

        return ParsedPaymentSms(
            provider = "tap",
            sender = sender,
            type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = "",
            suggestedCategory = if (isReceived) "other_income" else "other_expense",
            timestamp = timestamp,
            isConfident = trxId != null,
            note = "tap ${if (isReceived) "Received Funds" else "Payment"}"
        )
    }

    private fun parseMcash(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)
        val isReceived = body.contains("received", ignoreCase = true)

        return ParsedPaymentSms(
            provider = "mCash",
            sender = sender,
            type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = "",
            suggestedCategory = if (isReceived) "other_income" else "other_expense",
            timestamp = timestamp,
            isConfident = trxId != null,
            note = "mCash ${if (isReceived) "Received Money" else "Payment"}"
        )
    }

    private fun parseOkWallet(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)
        val isReceived = body.contains("received", ignoreCase = true)

        return ParsedPaymentSms(
            provider = "OK Wallet",
            sender = sender,
            type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = "",
            suggestedCategory = if (isReceived) "other_income" else "shopping",
            timestamp = timestamp,
            isConfident = trxId != null,
            note = "OK Wallet ${if (isReceived) "Received Money" else "Payment"}"
        )
    }

    private fun parseMyCash(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)
        val isReceived = body.contains("received", ignoreCase = true)

        return ParsedPaymentSms(
            provider = "MyCash",
            sender = sender,
            type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = "",
            suggestedCategory = if (isReceived) "other_income" else "other_expense",
            timestamp = timestamp,
            isConfident = trxId != null,
            note = "MyCash Transaction"
        )
    }

    private fun parseSureCash(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)
        val isReceived = body.contains("received", ignoreCase = true)

        return ParsedPaymentSms(
            provider = "SureCash",
            sender = sender,
            type = if (isReceived) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = "",
            suggestedCategory = if (isReceived) "other_income" else "other_expense",
            timestamp = timestamp,
            isConfident = trxId != null,
            note = "SureCash Transaction"
        )
    }

    private fun parseBankOrCard(sender: String, body: String, timestamp: Long): ParsedPaymentSms? {
        val amount = extractAmount(body) ?: return null
        val fee = extractFee(body)
        val trxId = extractTrxId(body)

        val isCardBillPayment = body.contains("Credit Card", ignoreCase = true) &&
                (body.contains("payment of", ignoreCase = true) || body.contains("payment received", ignoreCase = true) || body.contains("bill payment", ignoreCase = true))

        val isRefundOrReversal = body.contains("refund", ignoreCase = true) || body.contains("reversed", ignoreCase = true) || body.contains("reversal", ignoreCase = true)

        val isDebited = !isRefundOrReversal && (body.contains("debited", ignoreCase = true) || body.contains("used for", ignoreCase = true) ||
                body.contains("purchased", ignoreCase = true) || body.contains("withdrawn", ignoreCase = true) || body.contains("paid", ignoreCase = true))

        val isCredited = isRefundOrReversal || body.contains("credited", ignoreCase = true) || body.contains("deposited", ignoreCase = true) ||
                (!isCardBillPayment && body.contains("received", ignoreCase = true))

        if (!isDebited && !isCredited && !isCardBillPayment) return null

        val isCreditCardPurchase = body.contains("Credit Card", ignoreCase = true) && !isCardBillPayment
        val isDebitCardPurchase = body.contains("Debit Card", ignoreCase = true)
        val isAtmWithdrawal = body.contains("ATM", ignoreCase = true) || body.contains("withdrawn", ignoreCase = true)

        val type = if (isCredited) TransactionType.INCOME else TransactionType.EXPENSE

        val counterparty = when {
            body.contains(" at ", ignoreCase = true) -> {
                body.substringAfter(" at ", "").substringBefore(" on").substringBefore(".").trim().take(40)
            }
            body.contains("Card ", ignoreCase = true) -> {
                body.substringAfter("Card ", "").substringBefore(" was").substringBefore(".").trim().take(30)
            }
            else -> ""
        }

        val note = when {
            isCardBillPayment -> "Credit Card Bill Payment (review to avoid double counting)"
            isRefundOrReversal -> "${sender.ifBlank { "Bank" }} Refund/Reversal"
            isAtmWithdrawal -> "${sender.ifBlank { "Bank" }} Cash Withdrawal${if (counterparty.isNotBlank()) " at $counterparty" else ""}"
            isCreditCardPurchase -> "Credit Card Purchase${if (counterparty.isNotBlank()) " at $counterparty" else ""}"
            isDebitCardPurchase -> "Debit Card Purchase${if (counterparty.isNotBlank()) " at $counterparty" else ""}"
            isDebited -> "${sender.ifBlank { "Bank" }} Debit${if (counterparty.isNotBlank()) " at $counterparty" else ""}"
            isCredited -> "${sender.ifBlank { "Bank" }} Credit"
            else -> "Bank Transaction"
        }

        // Bill payments and ambiguous messages are never auto-posted (isConfident = false)
        val isConfident = trxId != null && !isCardBillPayment

        val suggestedCategory = when {
            isCredited -> "other_income"
            isCardBillPayment -> "bills"
            isCreditCardPurchase || isDebitCardPurchase -> "shopping"
            isAtmWithdrawal -> "daily_expense"
            else -> "other_expense"
        }

        return ParsedPaymentSms(
            provider = if (sender.isNotBlank()) sender.take(20) else "Bank",
            sender = sender,
            type = type,
            amount = amount,
            fee = fee,
            trxId = trxId,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory,
            timestamp = timestamp,
            isConfident = isConfident,
            note = note,
            isCardBillPayment = isCardBillPayment
        )
    }
}
