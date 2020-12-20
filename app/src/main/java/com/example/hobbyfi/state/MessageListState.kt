package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.responses.Response

// TODO: Fix this redeclaration of States and find a way to create a generic responseState
sealed class MessageListState : State {
    object Idle : MessageListState()
    object Loading : MessageListState()

    data class MessagesResult(val messages: PagingData<Message>) // TODO: if fetch from network the map<String, Message> will be converted to a flat list and processed by pagination

    data class Error(val error: String?, val shouldReauth: Boolean = false) : MessageListState()
}