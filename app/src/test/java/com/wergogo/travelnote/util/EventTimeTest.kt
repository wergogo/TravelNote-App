package com.wergogo.travelnote.util

import com.wergogo.travelnote.data.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventTimeTest {

    @Test
    fun `single-digit hours are zero padded`() {
        assertEquals("09:03", normalizeTime("9:03"))
    }

    @Test
    fun `already padded times are unchanged`() {
        assertEquals("18:11", normalizeTime("18:11"))
        assertEquals("00:00", normalizeTime("0:00"))
    }

    @Test
    fun `surrounding spaces are ignored`() {
        assertEquals("07:45", normalizeTime(" 7:45 "))
    }

    @Test
    fun `out-of-range or malformed times are rejected`() {
        listOf("24:00", "12:60", "9:3", "930", "", "ab:cd", "1:2:3").forEach { input ->
            assertNull("\"$input\" should be rejected", normalizeTime(input))
        }
    }

    @Test
    fun `events sort by time of day, earliest first`() {
        val sorted = listOf(event("a", "18:11"), event("b", "21:22"), event("c", "9:03"))
            .sortedWith(EventChronology)

        assertEquals(listOf("c", "a", "b"), sorted.map { it.id })
    }

    @Test
    fun `events with an unreadable time go last, in creation order`() {
        val sorted = listOf(event("x", "", createdAt = 1), event("y", "10:00", createdAt = 2), event("z", "??", createdAt = 3))
            .sortedWith(EventChronology)

        assertEquals(listOf("y", "x", "z"), sorted.map { it.id })
    }

    @Test
    fun `events at the same time keep creation order`() {
        val sorted = listOf(event("second", "09:00", createdAt = 2), event("first", "9:00", createdAt = 1))
            .sortedWith(EventChronology)

        assertEquals(listOf("first", "second"), sorted.map { it.id })
    }

    private fun event(id: String, time: String, createdAt: Long = 0) =
        Event(id = id, time = time, createdAt = createdAt)
}
