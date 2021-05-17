package com.example.hobbyfi.intents


sealed class EventIntent : Intent {
    data class CreateEvent(val chatroomId: Long) : EventIntent()
    data class UpdateEvent(val eventUpdateFields: Map<String, String?>) : EventIntent()

    data class DeleteEvent(val eventId: Long) : EventIntent()
}