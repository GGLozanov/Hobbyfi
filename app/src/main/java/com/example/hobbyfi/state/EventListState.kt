package com.example.hobbyfi.state

import com.example.hobbyfi.models.Event
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.StartDateIdResponse

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class EventListState : State {
    object Idle : EventListState()
    object Loading : EventListState()

    sealed class OnData : EventListState() {
        data class EventsResult(val events: List<Event>) : EventListState()

        object DeleteEventsCacheResult : OnData()
        object DeleteAnEventCacheResult : OnData()
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : EventListState()
}