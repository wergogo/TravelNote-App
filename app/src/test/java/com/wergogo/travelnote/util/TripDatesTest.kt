package com.wergogo.travelnote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDatesErrorTest {
    @Test
    fun `a valid range has no error`() {
        assertNull(tripDatesError("2025-03-01", "2025-03-03"))
        assertNull(tripDatesError("2025-03-01", "2025-03-01"))
    }

    @Test
    fun `missing, reversed and malformed dates are reported`() {
        assertEquals("Dates are required", tripDatesError("", "2025-03-01"))
        assertEquals("End date cannot be before start date", tripDatesError("2025-03-05", "2025-03-01"))
        assertEquals("Invalid date format", tripDatesError("03/01/2025", "2025-03-03"))
    }
}

class GenerateDateKeysTest {
    @Test
    fun `normal range includes both ends`() {
        assertEquals(
            listOf("2025-03-01", "2025-03-02", "2025-03-03"),
            generateDateKeys("2025-03-01", "2025-03-03")
        )
    }

    @Test
    fun `single day range returns that day`() {
        assertEquals(listOf("2025-03-01"), generateDateKeys("2025-03-01", "2025-03-01"))
    }

    @Test
    fun `end before start returns empty list`() {
        assertTrue(generateDateKeys("2025-03-05", "2025-03-01").isEmpty())
    }

    @Test
    fun `bad input returns empty list`() {
        assertTrue(generateDateKeys("", "").isEmpty())
        assertTrue(generateDateKeys("not-a-date", "2025-03-01").isEmpty())
        assertTrue(generateDateKeys("2025-03-01", "2025-13-45").isEmpty())
        assertTrue(generateDateKeys("03/01/2025", "03/03/2025").isEmpty())
    }

    @Test
    fun `range crossing a month boundary in a leap year includes the 29th`() {
        assertEquals(
            listOf("2024-02-28", "2024-02-29", "2024-03-01"),
            generateDateKeys("2024-02-28", "2024-03-01")
        )
    }

    @Test
    fun `range crossing a year boundary is contiguous`() {
        assertEquals(
            listOf("2024-12-31", "2025-01-01"),
            generateDateKeys("2024-12-31", "2025-01-01")
        )
    }
}
