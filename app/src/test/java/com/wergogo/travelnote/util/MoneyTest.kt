package com.wergogo.travelnote.util

import com.wergogo.travelnote.DefaultLocaleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class MoneyTest {
    @get:Rule val localeRule = DefaultLocaleRule()

    // ── parseAmount ──────────────────────────────────────────────────────────

    @Test
    fun `plain numbers and decimal points parse as usual`() {
        assertEquals(12.5, parseAmount("12.50")!!, 0.0)
        assertEquals(1500.0, parseAmount("1500")!!, 0.0)
        assertEquals(0.5, parseAmount(".5")!!, 0.0)
        assertEquals(42.0, parseAmount(" 42 ")!!, 0.0)
    }

    @Test
    fun `a comma followed by one or two final digits is the decimal separator`() {
        assertEquals(12.5, parseAmount("12,50")!!, 0.0)
        assertEquals(7.5, parseAmount("7,5")!!, 0.0)
        assertEquals(1500.25, parseAmount("1.500,25")!!, 0.0)
    }

    @Test
    fun `other commas are thousands separators`() {
        assertEquals(1500.0, parseAmount("1,500")!!, 0.0)
        assertEquals(1500.25, parseAmount("1,500.25")!!, 0.0)
        assertEquals(1234567.0, parseAmount("1,234,567")!!, 0.0)
    }

    @Test
    fun `the result does not depend on the device locale`() {
        Locale.setDefault(Locale.GERMANY)   // restored by localeRule
        assertEquals(12.5, parseAmount("12,50")!!, 0.0)
        assertEquals(1500.0, parseAmount("1,500")!!, 0.0)
        assertEquals(1500.25, parseAmount("1,500.25")!!, 0.0)
    }

    @Test
    fun `text that is not a plain amount is rejected`() {
        listOf("", "   ", "abc", "1,5000", "12,5,0", "1,50,000", "-5", "1e3", "NaN", "Infinity", "1.2.3", ",5")
            .forEach { input -> assertNull("\"$input\" should be rejected", parseAmount(input)) }
    }

    // ── amountInputText ──────────────────────────────────────────────────────

    @Test
    fun `stored amounts are shown as plain text the parser accepts`() {
        assertEquals("320", amountInputText(320.0))
        assertEquals("18.75", amountInputText(18.75))
        assertEquals("10000000", amountInputText(1.0E7))   // Double.toString would give "1.0E7"
        assertEquals("0", amountInputText(0.0))
        for (value in listOf(320.0, 18.75, 1.0E7, 0.0, 1500.5)) {
            assertEquals(value, parseAmount(amountInputText(value))!!, 0.0)
        }
    }

    // ── formatMoney ──────────────────────────────────────────────────────────

    @Test
    fun `money is shown as currency then two decimals`() {
        assertEquals("EUR 1234.50", formatMoney("EUR", 1234.5))
    }

    @Test
    fun `a currency containing a percent sign is shown, not treated as a format code`() {
        assertEquals("%x 12.50", formatMoney("%x", 12.5))
    }
}
