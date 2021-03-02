package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.User

sealed class UserListState : State {
    object Idle : UserListState()
    object Loading : UserListState()

    sealed class OnData : UserListState() {
        data class UsersResult(val users: List<User>) : OnData()
    }

    data class OnUserKick(val userKickedId: Long) : UserListState()

    // TODO: Find a way to Swift-ify this and pass the eventId only in OnNotification and have the inner classes access it
    data class Error(val error: String?, val shouldReauth: Boolean = false) : UserListState()
}
