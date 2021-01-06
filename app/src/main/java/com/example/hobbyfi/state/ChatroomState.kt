package com.example.hobbyfi.state

import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.responses.IdResponse
import com.example.hobbyfi.responses.Response

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class ChatroomState : State {
    object Idle : ChatroomState()
    object Loading : ChatroomState()

    sealed class OnData : ChatroomState() {
        data class ChatroomDeleteResult(val response: Response?) : OnData()
        data class ChatroomCreateResult(val response: Chatroom) : OnData()
        data class ChatroomUpdateResult(val response: Response?, val fieldMap: Map<String?, String?>) : OnData()
        data class ChatroomResult(val chatroom: Chatroom) : OnData()

        object DeleteChatroomCacheResult : OnData()
        // no need for UpdateChatroomCacheResult state because it always succeeds
    }

    data class Error(val error: String?, val shouldExit: Boolean = false) : ChatroomState()
}