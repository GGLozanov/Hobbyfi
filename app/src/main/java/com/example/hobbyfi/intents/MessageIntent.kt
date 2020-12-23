package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Message

// CRUD intents
sealed class MessageIntent : Intent {
    data class CreateMessage(val message: String, val userId: Long, val chatroomId: Long) : MessageIntent() // created with two-way databinding in viewmodel

    data class UpdateMessage(val messageId: Int) // message text content received again by databinding. Handle when user enters nothing in message box
    data class DeleteMessage(val messageId: Int) : MessageIntent()
}