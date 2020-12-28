package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Chatroom

// fire off request for chatroom info upon entering mainActivity
// encapsulates chatroom tag fetching (chatroom info fetch intent)
// includes delete/edit chatroom intents
// and includes instant delete chatroom intent
sealed class ChatroomIntent : Intent {
    object FetchChatroom : ChatroomIntent()
    data class CreateChatroom(val ownerId: Long) : ChatroomIntent()

    data class UpdateChatroom(val chatroomUpdateFields: Map<String?, String?>) : ChatroomIntent()

    object DeleteChatroomCache : ChatroomIntent()
    data class UpdateChatroomCache(val chatroomUpdateFields: Map<String?, String?>) : ChatroomIntent()

    object DeleteChatroom : ChatroomIntent()
}