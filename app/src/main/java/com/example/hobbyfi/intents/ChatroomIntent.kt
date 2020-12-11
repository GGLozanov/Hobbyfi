package com.example.hobbyfi.intents

// fire off request for chatroom info upon entering mainActivity
// encapsulates chatroom tag fetching (chatroom info fetch intent)
// includes delete/edit chatroom intents
// and includes instant delete chatroom intent
sealed class ChatroomIntent : Intent {
    object FetchChatroom : ChatroomIntent()
    object CreateChatroom : ChatroomIntent()
    data class UpdateChatroom(val chatroomUpdateFields: Map<String, String>) : ChatroomIntent()
    object DeleteChatroom : ChatroomIntent()
}