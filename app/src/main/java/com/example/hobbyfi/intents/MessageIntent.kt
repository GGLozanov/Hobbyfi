package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Message

// CRUD intents
sealed class MessageIntent : Intent {
    sealed class FetchMessages(val chatroomId: Int) : MessageIntent()

    object CreateMessage : MessageIntent() // created with two-way databinding in viewmodel
    sealed class UpdateMessage(val messageId: Int) // message text content received again by databinding. Handle when user enters nothing in message box
    sealed class DeleteMessage(val messageId: Int) : MessageIntent()
}