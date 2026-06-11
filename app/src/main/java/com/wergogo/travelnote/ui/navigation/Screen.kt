package com.wergogo.travelnote.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object CreateTrip : Screen("trip/create")
    object JoinTrip : Screen("trip/join")

    object TripOverview : Screen("trip/{tripId}") {
        fun route(tripId: String) = "trip/$tripId"
    }
    object EditTrip : Screen("trip/{tripId}/edit") {
        fun route(tripId: String) = "trip/$tripId/edit"
    }
    object DayDetail : Screen("trip/{tripId}/day/{dateKey}") {
        fun route(tripId: String, dateKey: String) = "trip/$tripId/day/$dateKey"
    }
    object AddEvent : Screen("trip/{tripId}/day/{dateKey}/event?eventId={eventId}") {
        fun route(tripId: String, dateKey: String, eventId: String? = null): String {
            val base = "trip/$tripId/day/$dateKey/event"
            return if (eventId != null) "$base?eventId=$eventId" else base
        }
    }
    object Members : Screen("trip/{tripId}/members") {
        fun route(tripId: String) = "trip/$tripId/members"
    }
}
