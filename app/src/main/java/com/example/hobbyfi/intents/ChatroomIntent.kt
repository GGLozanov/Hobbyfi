package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Chatroom

sealed class ChatroomIntent : Intent {
    object FetchChatroom : ChatroomIntent()
    data class CreateChatroom(val ownerId: Long) : ChatroomIntent()

    data class UpdateChatroom(val chatroomUpdateFields: Map<String, String?>) : ChatroomIntent()

    data class DeleteChatroomCache(val kicked: Boolean = false) : ChatroomIntent()
    data class UpdateChatroomCache(val chatroomUpdateFields: Map<String, String?>) : ChatroomIntent()

    object DeleteChatroom : ChatroomIntent()
}