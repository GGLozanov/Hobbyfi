package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Event

sealed class EventListIntent : Intent {
    object FetchEvents : EventListIntent()

    data class UpdateAnEventCache(val eventUpdateFields: Map<String?, String?>) : EventListIntent()
    data class DeleteAnEventCache(val eventId: Long) : EventListIntent()

    data class DeleteEventsCache(val eventIds: List<Long>) : EventListIntent()
    object DeleteOldEventsCache : EventListIntent()
}