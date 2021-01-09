package com.example.hobbyfi.state

import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.StartDateIdResponse

sealed class EventState : State {
    object Idle : EventState()
    object Loading : EventState()

    sealed class OnData : EventState() {
        data class EventEditResult(val response: Response?) : OnData()
        data class EventDeleteResult(val response: Response?) : OnData()
        data class EventCreateResult(val response: StartDateIdResponse?) : OnData()

        object DeleteEventCacheResult : OnData()
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : EventState()
}