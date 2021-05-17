package com.example.hobbyfi.state

import com.example.hobbyfi.models.data.Event

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class EventListState : State {
    object Idle : EventListState()
    object Loading : EventListState()

    sealed class OnData : EventListState() {
        data class EventsResult(val events: List<Event>) : OnData()
        data class DeleteOldEventsResult(val deletedEventsId: List<Long>) : OnData()

        data class DeleteEventsCacheResult(val eventIds: List<Long>) : OnData()
        data class DeleteAnEventCacheResult(val eventId: Long) : OnData()
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : EventListState()
}