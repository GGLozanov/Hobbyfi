package com.example.hobbyfi.state

import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Event

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class EventState : State {
    object Idle : EventState()
    object Loading : EventState()

    data class OnEventReceived(val event: Event) : EventState()

    // TODO: Find a way to Swift-ify this and pass the eventId only in OnNotification and have the inner classes access it
    sealed class OnNotification : EventState() {
        data class OnCreateEventNotification(val event: Event) : OnNotification() // push notification + data => update UI
        data class OnDeleteEventNotification(val eventId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom delete (in chatroom & main activity) for all users
        data class OnUpdateEventNotification(val eventId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom update for all users
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : EventState()
}