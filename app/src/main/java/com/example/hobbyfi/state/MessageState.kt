package com.example.hobbyfi.state

import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class MessageState {
    object Idle : MessageState()
    object Loading : MessageState()

    sealed class OnData : MessageState() {
        data class MessageResult(val messages: List<Message>) // TODO: if fetch from network the map<String, Message> will be converted to a flat list
    }

    // TODO: Find a way to Swift-ify this and pass the eventId only in OnNotification and have the inner classes access it
    sealed class OnNotification : MessageState() {
        data class OnNewMessageNotification(val message: Message) // triggered by broadcastreceiver in activity/fragment from FCM notifications => live message creation for all users
        data class OnDeleteMessageNotification(val messageId: Int) // triggered by broadcastreceiver in activity/fragment from FCM notifications => live message delete for all users
        data class OnUpdateMessageNotification(val messageId: Int) // triggered by broadcastreceiver in activity/fragment from FCM notifications => live message update for all users
    }

    data class Error(val error: String?) : MessageState()
}