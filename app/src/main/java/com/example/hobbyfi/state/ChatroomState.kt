package com.example.hobbyfi.state

import com.example.hobbyfi.models.Chatroom

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class ChatroomState {
    object Idle : ChatroomState()
    object Loading : ChatroomState()

    sealed class OnData : ChatroomState() {
        data class ChatroomResult(val chatroom: Chatroom) // might not need this
        data class ChatroomsResult(val chatrooms: List<Chatroom>) // TODO: if fetch from network the map<String, Chatroom> will be converted to a flat list
    }

    // TODO: Find a way to Swift-ify this and pass the chatroomId only in OnNotification and have the inner classes access it
    sealed class OnNotification : ChatroomState() {
        data class OnDeleteChatroomNotification(val chatroomId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom delete (in chatroom & main activity) for all users
        data class OnUpdateChatroomNotification(val chatroomId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom update for all users
    }

    data class Error(val error: String?) : ChatroomState()
}