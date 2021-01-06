package com.example.hobbyfi.intents

import android.net.Uri
import com.example.hobbyfi.models.Message

// CRUD intents
sealed class MessageIntent : Intent {
    data class CreateMessage(val message: String? = null, val userSentId: Long, val chatroomSentId: Long) : MessageIntent()
        // created with two-way databinding in viewmodel
    data class CreateMessageImages(val base64s: List<String>, val userSentId: Long, val chatroomSentId: Long) : MessageIntent()

    data class UpdateMessage(val messageUpdateFields: Map<String?, String?>) : MessageIntent()
        // message text content received again by databinding. Handle when user enters nothing in message box
    data class DeleteMessage(val messageId: Long) : MessageIntent()

    data class UpdateMessageCache(val messageUpdateFields: Map<String?, String?>) : MessageIntent()
    data class DeleteMessageCache(val messageId: Long) : MessageIntent()

    data class CreateMessageCache(val message: Message) : MessageIntent()
}