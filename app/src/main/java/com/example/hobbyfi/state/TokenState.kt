package com.example.hobbyfi.state

import com.example.hobbyfi.responses.TokenResponse


// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class TokenState : State {
    object Idle : TokenState()
    object Loading : TokenState()

    data class OnTokenReceived(val token: TokenResponse?) : TokenState()

    data class Error(val error: String?) : TokenState()
}