package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Event

sealed class EventListIntent : Intent {
    object FetchEvents : EventListIntent()

    data class AddAnEventCache(val event: Event) : EventListIntent()
    data class UpdateAnEventCache(val eventUpdateFields: Map<String?, String?>) : EventListIntent()
    data class DeleteAnEventCache(val eventId: Long) : EventListIntent()

    object RefetchEvent : EventListIntent()

    data class DeleteEventsCache(val eventIds: List<Long>) : EventListIntent()
    object DeleteOldEvents : EventListIntent()
}