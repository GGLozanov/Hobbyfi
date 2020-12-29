package com.example.hobbyfi.intents

sealed class MessageListIntent : Intent {
    object FetchMessages : MessageListIntent()
}
