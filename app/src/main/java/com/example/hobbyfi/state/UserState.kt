package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.User
import com.example.hobbyfi.responses.Response

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class UserState : State {
    object Idle : UserState()
    object Loading : UserState()

    sealed class OnData : UserState() {
        data class UserResult(val user: User) : OnData()
        data class UserUpdateResult(val response: Response?, val userFields: Map<String?, String?>) : OnData()
        data class UserDeleteResult(val response: Response?) : OnData()
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : UserState()
}