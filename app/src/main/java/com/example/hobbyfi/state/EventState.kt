package com.example.hobbyfi.state

import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.StartDateIdResponse

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class EventState : State {
    object Idle : EventState()
    object Loading : EventState()

    sealed class OnData : EventState() {
        data class EventResult(val event: Event) : EventState()
        data class EventEditResult(val response: Response?) : EventState()
        data class EventDeleteResult(val response: Response?) : EventState()
        data class EventCreateResult(val response: StartDateIdResponse?) : EventState()

        object DeleteEventCacheResult : OnData()
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : EventState()
}