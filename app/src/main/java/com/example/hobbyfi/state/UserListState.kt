package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.User

sealed class UserListState : State {
    object Idle : UserListState()
    object Loading : UserListState()

    data class UsersResult(val users: PagingData<User>) : UserListState() // TODO: if fetch from network the map<String, User> will be converted to a flat list and processed by pagination

    // TODO: Find a way to Swift-ify this and pass the eventId only in OnNotification and have the inner classes access it
    sealed class OnNotification : UserListState() {
        data class ChatroomJoinNotification(val user: User) : OnNotification() // entire user model is sent through the notification (FIXME: Bad idea and misue of FCM...?)
        data class ChatroomLeaveNotification(val username: String) : OnNotification() // only need username to notify users of someone leaving
        // TODO: OnDeleteUserNotification special case: can trigger OnDeleteChatroomNotification if deleted user is chatroom owner
        data class DeleteUserNotification(val userId: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live user delete (in chatroom & main activity) for all users
        data class UpdateUserNotification(val userid: Int) : OnNotification() // triggered by broadcastreceiver in activity/fragment from FCM notifications => live chatroom update for all users
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : UserListState()
}
