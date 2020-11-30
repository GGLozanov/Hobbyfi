package com.example.hobbyfi.state

import com.example.hobbyfi.models.Tag

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class FacebookState : State {
    object Idle : FacebookState()
    object Loading : FacebookState()

    sealed class OnData : FacebookState() {
        data class TagsReceived(val tags: List<Tag>) : FacebookState()
        data class EmailReceived(val email: String?) : FacebookState()
        data class ExistenceResultReceived(val exists: Boolean) : FacebookState()
    }

    data class Error(val error: String?) : FacebookState()
}