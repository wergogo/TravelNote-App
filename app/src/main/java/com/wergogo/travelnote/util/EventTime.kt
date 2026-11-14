package com.wergogo.travelnote.util

import com.wergogo.travelnote.data.model.Event
import java.util.Locale

private val TIME_OF_DAY = Regex("""(\d{1,2}):(\d{2})""")

/** Minutes since midnight for an "H:mm" or "HH:mm" time, or null if it isn't a valid time of day. */
fun minutesOfDay(time: String): Int? {
    val match = TIME_OF_DAY.matchEntire(time.trim()) ?: return null
    val hours = match.groupValues[1].toInt()
    val minutes = match.groupValues[2].toInt()
    if (hours > 23 || minutes > 59) return null
    return hours * 60 + minutes
}

/** "9:03" → "09:03", so times also sort correctly as text; null if [raw] isn't a valid time of day. */
fun normalizeTime(raw: String): String? =
    minutesOfDay(raw)?.let { String.format(Locale.ROOT, "%02d:%02d", it / 60, it % 60) }

/**
 * Earliest event first, by actual time of day. Times saved before 1.0.2 may be unpadded ("9:03"),
 * so they're compared as numbers, not text. Unreadable times go last; ties keep creation order.
 */
val EventChronology: Comparator<Event> =
    compareBy<Event> { minutesOfDay(it.time) ?: Int.MAX_VALUE }.thenBy { it.createdAt }
