package com.example.hobbyfi.state

import com.example.hobbyfi.responses.Response

sealed class MessageState : State {
    object Idle : MessageState()
    object Loading : MessageState()

    sealed class OnData : State {
        data class MessageCreateResult(val response: Response)
        data class MessageEditResult(val response: Response)
        data class MessageDeleteResult(val response: Response)
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : MessageState()
}
