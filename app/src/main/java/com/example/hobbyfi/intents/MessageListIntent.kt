package com.example.hobbyfi.intents

sealed class MessageListIntent : Intent {
    data class FetchMessages(val chatroomId: Long, val query: String? = null) : MessageListIntent()
}
