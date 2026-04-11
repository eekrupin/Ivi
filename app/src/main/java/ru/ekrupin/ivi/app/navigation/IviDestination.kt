package ru.ekrupin.ivi.app.navigation

sealed class IviDestination(val route: String) {
    data object Home : IviDestination("home")
    data object Events : IviDestination("events")
    data object Weight : IviDestination("weight")
    data object EventTypes : IviDestination("event_types")
    data object Settings : IviDestination("settings")

    data object EventEdit : IviDestination("event_edit?eventId={eventId}") {
        fun createRoute(eventId: Long? = null): String =
            eventId?.let { "event_edit?eventId=$it" } ?: "event_edit"
    }
}
