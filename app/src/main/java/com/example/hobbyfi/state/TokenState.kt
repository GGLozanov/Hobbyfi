package com.example.hobbyfi.state

import com.example.hobbyfi.models.Token

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class TokenState {
    object Idle : TokenState()
    object Loading : TokenState()

    data class OnTokenReceived(val token: Token)

    data class Error(val error: String?) : TokenState()
}