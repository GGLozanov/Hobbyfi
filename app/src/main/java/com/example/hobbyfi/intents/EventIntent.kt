package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Event

// fire off request for last event info upon entering chatroom activity
// TODO: Add fetch intent, database location polling intent, location update intent, etc. here?
sealed class EventIntent : Intent {
    sealed class FetchLastEventIntent(val chatroomId: Int) : EventIntent()

    sealed class CreateEvent : EventIntent() // event information received by event create form in viewModel + EventCreateMapsActivity callback for lat/long
    sealed class DeleteEvent(val eventId: Int) : EventIntent()
    sealed class UpdateEvent(val eventUpdateFields: Map<String, String>) : EventIntent()
}