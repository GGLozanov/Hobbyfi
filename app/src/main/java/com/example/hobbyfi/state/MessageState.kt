package com.example.hobbyfi.state

import com.example.hobbyfi.responses.CreateTimeIdResponse
import com.example.hobbyfi.responses.IdResponse
import com.example.hobbyfi.responses.Response

sealed class MessageState : State {
    object Idle : MessageState()
    object Loading : MessageState()

    sealed class OnData : MessageState() {
        data class MessageCreateResult(val response: CreateTimeIdResponse?) : OnData()
        data class MessageUpdateResult(val response: Response?) : OnData()
        data class MessageDeleteResult(val response: Response?) : OnData()

        object DeleteMessageCacheResult : OnData()
    }

    data class Error(val error: String?, val shouldExit: Boolean = false) : MessageState()
}
