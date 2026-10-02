package com.example.ui.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val wholeFormatter = DecimalFormat("#,##0")
    private val decimalFormatter = DecimalFormat("#,##0.00")

    fun formatBDT(amount: Double, includeDecimalsIfZero: Boolean = false): String {
        val hasDecimals = (amount % 1.0) != 0.0
        val formattedNumber = if (hasDecimals || includeDecimalsIfZero) {
            decimalFormatter.format(amount)
        } else {
            wholeFormatter.format(amount)
        }
        return "৳$formattedNumber"
    }

    fun formatBDTWithSign(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign${formatBDT(amount)}"
    }
}
