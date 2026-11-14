package com.wergogo.travelnote.util

import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Every day of a trip as "yyyy-MM-dd", start and end included; empty if the range is invalid. */
fun generateDateKeys(startDate: String, endDate: String): List<String> {
    return try {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.toString() }
            .toList()
    } catch (_: DateTimeParseException) {
        emptyList()
    }
}

/** Validation message for a trip's "yyyy-MM-dd" date range, or null if it's valid. */
fun tripDatesError(startDate: String, endDate: String): String? {
    if (startDate.isBlank() || endDate.isBlank()) return "Dates are required"
    return try {
        if (LocalDate.parse(endDate).isBefore(LocalDate.parse(startDate))) {
            "End date cannot be before start date"
        } else {
            null
        }
    } catch (_: DateTimeParseException) {
        "Invalid date format"
    }
}
