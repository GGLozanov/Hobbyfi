package com.example.hobbyfi.models.ui

import com.example.hobbyfi.models.data.Message

sealed class UIMessage {
    data class MessageItem(val message: Message) : UIMessage()

    data class MessageSeparatorItem(val dateText: String) : UIMessage()
}