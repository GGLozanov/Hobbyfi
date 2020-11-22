package com.example.hobbyfi.state

import com.example.hobbyfi.responses.TokenResponse

sealed class ResponseState<out T> : State {
    object Idle : ResponseState<Nothing>()
    object Loading : ResponseState<Nothing>()

    data class OnData<T>(val data: T?) : ResponseState<T>()

    data class Error(val error: String?, val shouldReauth: Boolean = false): ResponseState<Nothing>()
}