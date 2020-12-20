package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.User

sealed class UserListState : State {
    object Idle : UserListState()
    object Loading : UserListState()

    data class UsersResult(val users: PagingData<User>) : UserListState() // TODO: if fetch from network the map<String, User> will be converted to a flat list and processed by pagination

    // TODO: Find a way to Swift-ify this and pass the eventId only in OnNotification and have the inner classes access it
    data class Error(val error: String?, val shouldReauth: Boolean = false) : UserListState()
}
