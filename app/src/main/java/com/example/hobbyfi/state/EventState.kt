package com.example.hobbyfi.state

import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Event

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class EventState : State {
    object Idle : EventState()
    object Loading : EventState()

    data class OnEventReceived(val event: Event) : EventState()

    data class Error(val error: String?, val shouldReauth: Boolean = false) : EventState()
}