package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Message

sealed class MessageListIntent : Intent {
    data class FetchMessages(val chatroomId: Long, val query: String? = null, val messageId: Long? = null) : MessageListIntent()
}
