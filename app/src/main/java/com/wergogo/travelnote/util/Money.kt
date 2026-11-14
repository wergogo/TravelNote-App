package com.wergogo.travelnote.util

import java.math.BigDecimal

// "12,50", "1.500,25": optional dot thousands groups, then a comma and one or two decimals.
private val DECIMAL_COMMA = Regex("""(\d{1,3}(?:\.\d{3})+|\d+),(\d{1,2})""")

// "1500", "1,500.25", ".5": optional comma thousands groups, then an optional decimal point.
private val DECIMAL_POINT = Regex("""(\d{1,3}(?:,\d{3})+|\d*)(?:\.(\d*))?""")

/**
 * Parses an amount typed by the user, the same way whatever the device language:
 * - a comma followed by exactly one or two final digits is the decimal separator ("12,50" → 12.5),
 * - any other comma separates thousands ("1,500" → 1500.0, "1,500.25" → 1500.25).
 *
 * Returns null for anything else (signs, exponents, "NaN", misplaced separators), so callers can
 * show a validation error instead of silently saving 0.
 */
fun parseAmount(raw: String): Double? {
    val text = raw.trim()
    DECIMAL_COMMA.matchEntire(text)?.let { match ->
        val whole = match.groupValues[1].replace(".", "")
        return "$whole.${match.groupValues[2]}".toDouble()
    }
    val match = DECIMAL_POINT.matchEntire(text) ?: return null
    val whole = match.groupValues[1].replace(",", "")
    val fraction = match.groupValues[2]
    if (whole.isEmpty() && fraction.isEmpty()) return null
    return "${whole.ifEmpty { "0" }}.${fraction.ifEmpty { "0" }}".toDouble()
}

/** A stored amount as plain input text that [parseAmount] accepts: "320", "18.75", never "1.0E7". */
fun amountInputText(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

/** "EUR 1234.50". The currency is user text, so it's never used as part of a format pattern. */
fun formatMoney(currency: String, amount: Double): String = "$currency ${"%.2f".format(amount)}"
