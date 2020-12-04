package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.Chatroom

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class ChatroomState : State {
    object Idle : ChatroomState()
    object Loading : ChatroomState()

    data class ChatroomResult(val chatroom: Chatroom) // might not need this...?

    // TODO: Find a way to Swift-ify this and pass the chatroomId only in OnNotification and have the inner classes access it
    sealed class OnNotification : ChatroomState() {
        data class DeleteChatroomNotification(val chatroomId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom delete (in chatroom & main activity) for all users
        data class UpdateChatroomNotification(val chatroomId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom update for all users
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : ChatroomState()
}