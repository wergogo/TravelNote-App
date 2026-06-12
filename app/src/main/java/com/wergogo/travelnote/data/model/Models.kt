package com.wergogo.travelnote.data.model

enum class EventType { FLIGHT, ACTIVITY }

enum class ExpenseCategory {
    FOOD, TRANSPORT, SHOPPING, ACCOMMODATION, OTHER;

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class TripVisibility { OPEN, LOCKED }

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val nickname: String = "",
    val avatarId: String = "avatar_01",
    val createdAt: Long = 0L
)

data class Trip(
    val id: String = "",
    val name: String = "",
    val destination: String = "",
    val startDate: String = "",   // "yyyy-MM-dd"
    val endDate: String = "",     // "yyyy-MM-dd"
    val totalBudget: Double = 0.0,
    val currency: String = "USD",
    val visibility: TripVisibility = TripVisibility.OPEN,
    val joinCode: String = "",
    val coverId: String = "cover_mountains",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)

data class Member(
    val userId: String = "",
    val nickname: String = "",
    val avatarId: String = "avatar_01",
    val role: String = "member",  // "owner" | "member"
    val joinedAt: Long = 0L
)

data class Day(
    val date: String = "",         // "yyyy-MM-dd"
    val stayName: String = "",
    val stayCost: Double = 0.0
)

data class Event(
    val id: String = "",
    val type: EventType = EventType.ACTIVITY,
    val time: String = "",         // "HH:mm"
    val cost: Double = 0.0,
    // Flight fields
    val flightNumber: String = "",
    val depAirport: String = "",
    val arrAirport: String = "",
    // Activity fields
    val title: String = "",
    val locationName: String = "",
    // Author snapshot
    val authorId: String = "",
    val authorNickname: String = "",
    val authorAvatarId: String = "",
    val createdAt: Long = 0L
)

data class Expense(
    val id: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amount: Double = 0.0,
    val description: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val placeLabel: String = "",
    val authorId: String = "",
    val authorNickname: String = "",
    val authorAvatarId: String = "",
    val createdAt: Long = 0L
)

data class Note(
    val id: String = "",
    val text: String = "",
    val authorId: String = "",
    val authorNickname: String = "",
    val authorAvatarId: String = "",
    val createdAt: Long = 0L
)
