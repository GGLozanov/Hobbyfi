package com.example.hobbyfi.intents

// fire off request for chatroom info upon entering mainActivity
// encapsulates chatroom tag fetching (chatroom info fetch intent)
// includes delete/edit chatroom intents
// and includes instant delete chatroom intent
sealed class ChatroomIntent : Intent {
    object FetchChatroom : ChatroomIntent() // fetch registered user's chatroom => pass in chatroom id?

    data class UpdateChatroom(val chatroomId: Int, val chatroomUpdateFields: Map<String, String>) : ChatroomIntent()
    data class DeleteChatroom(val chatroomId: Int) : ChatroomIntent()
}