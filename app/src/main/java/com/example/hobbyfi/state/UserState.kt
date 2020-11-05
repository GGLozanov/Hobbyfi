package com.example.hobbyfi.state

import com.example.hobbyfi.models.User

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class UserState {
    object Idle : UserState()
    object Loading : UserState()

    // TODO: Find a way to Swift-ify this and pass the eventId only in OnNotification and have the inner classes access it
    sealed class OnNotification : UserState() {
        data class OnChatroomJoinNotification(val user: User) : OnNotification() // entire user model is sent through the notification (FIXME: Bad idea and misue of FCM...?)
        data class OnChatroomLeaveNotification(val username: String) : OnNotification() // only need username to notify users of someone leaving
        // TODO: OnDeleteUserNotification special case: can trigger OnDeleteChatroomNotification if deleted user is chatroom owner
        data class OnDeleteUserNotification(val userId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live user delete (in chatroom & main activity) for all users
        data class OnUpdateUserNotification(val userid: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom update for all users
    }

    data class Error(val error: String?) : UserState()
}