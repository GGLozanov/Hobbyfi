package com.example.hobbyfi.intents


sealed class EventIntent : Intent {
    object CreateEvent : EventIntent()
    data class UpdateEvent(val eventUpdateFields: Map<String?, String?>) : EventIntent()

    data class DeleteEvent(val eventId: Long) : EventIntent()
}