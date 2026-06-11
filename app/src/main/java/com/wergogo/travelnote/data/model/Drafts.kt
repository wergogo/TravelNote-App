package com.wergogo.travelnote.data.model

data class TripDraft(
    val name: String,
    val destination: String,
    val startDate: String,       // "yyyy-MM-dd"
    val endDate: String,         // "yyyy-MM-dd"
    val totalBudget: Double,
    val currency: String,
    val visibility: TripVisibility,
    val coverId: String
)

data class EventDraft(
    val type: EventType,
    val time: String,            // "HH:mm"
    val cost: Double,
    val flightNumber: String = "",
    val depAirport: String = "", // IATA 3-letter, uppercase
    val arrAirport: String = "", // IATA 3-letter, uppercase
    val title: String = "",
    val locationName: String = "",
    val eventId: String? = null  // null = add, non-null = edit existing
)

data class ExpenseDraft(
    val category: ExpenseCategory,
    val amount: Double,
    val description: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val placeLabel: String = ""
)

data class NoteDraft(
    val text: String
)
