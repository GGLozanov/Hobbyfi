package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Event

// fire off request for last event info upon entering chatroom activity
// TODO: Add fetch intent, database location polling intent, location update intent, etc. here?
sealed class EventIntent : Intent {
    object FetchEvent : EventIntent()

    object CreateEvent : EventIntent() // event information received by event create form in viewModel + EventCreateMapsActivity callback for lat/long

    data class DeleteEvent(val eventId: Long) : EventIntent()
    data class UpdateEvent(val eventUpdateFields: Map<String?, String?>) : EventIntent()

    data class UpdateEventCache(val eventUpdateFields: Map<String?, String?>) : EventIntent()
}