package com.example.hobbyfi.state

import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.responses.IdResponse
import com.example.hobbyfi.responses.Response

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class ChatroomState : State {
    object Idle : ChatroomState()
    object Loading : ChatroomState()

    sealed class OnData : ChatroomState() {
        data class ChatroomDeleteResult(val response: Response?) : OnData()
        data class ChatroomCreateResult(val response: IdResponse?) : OnData()
        data class ChatroomUpdateResult(val response: Response?, val fieldMap: Map<String?, String?>) : OnData()
        data class ChatroomResult(val chatroom: Chatroom) : OnData()
    }

    // TODO: Find a way to Swift-ify this and pass the chatroomId only in OnNotification and have the inner classes access it
    sealed class OnNotification : ChatroomState() {
        data class DeleteChatroomNotification(val chatroomId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom delete (in chatroom & main activity) for all users
        data class UpdateChatroomNotification(val chatroomId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom update for all users
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : ChatroomState()
}