package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.responses.Response

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class MessageListState : State {
    object Idle : MessageListState()
    object Loading : MessageListState()

    data class MessagesResult(val messages: PagingData<Message>) // TODO: if fetch from network the map<String, Message> will be converted to a flat list and processed by pagination

    // TODO: Find a way to Swift-ify this and pass the eventId only in OnNotification and have the inner classes access it
    sealed class OnNotification : MessageListState() {
        data class NewMessageNotification(val message: Message) // triggered by broadcastreceiver in activity/fragment from FCM notifications => live message creation for all users
        data class DeleteMessageNotification(val messageId: Int) // triggered by broadcastreceiver in activity/fragment from FCM notifications => live message delete for all users
        data class UpdateMessageNotification(val messageId: Int) // triggered by broadcastreceiver in activity/fragment from FCM notifications => live message update for all users
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : MessageListState()
}