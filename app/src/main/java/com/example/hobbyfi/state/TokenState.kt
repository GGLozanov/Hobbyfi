package com.example.hobbyfi.state

import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.TokenResponse


// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class TokenState : State {
    object Idle : TokenState()
    object Loading : TokenState()

    data class TokenReceived(val token: TokenResponse?) : TokenState()
    object FacebookRegisterTokenSuccess : TokenState()
    data class ResetPasswordSuccess(val response: Response?) : TokenState() // not needed for now but eh

    data class Error(val error: String?) : TokenState()
}