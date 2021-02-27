package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.responses.Response
import kotlinx.coroutines.flow.Flow

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class MessageListState : State {
    object Idle : MessageListState()
    object Loading : MessageListState()

    sealed class OnData : MessageListState() {
        data class MessagesResult(val messages: Flow<PagingData<Message>>, val queriedMessageId: Long? = null) : MessageListState()
    }

    data class Error(val error: String?, val shouldExit: Boolean = false) : MessageListState()
}