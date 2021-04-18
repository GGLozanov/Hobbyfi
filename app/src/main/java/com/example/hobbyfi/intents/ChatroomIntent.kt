package com.example.hobbyfi.intents

sealed class ChatroomIntent : Intent {
    data class FetchChatroom(val currentDestinationId: Int? = null) : ChatroomIntent()
    data class CreateChatroom(val ownerId: Long) : ChatroomIntent()

    data class UpdateChatroom(val chatroomUpdateFields: Map<String, String?>) : ChatroomIntent()

    data class DeleteChatroomCache(val kicked: Boolean = false) : ChatroomIntent()
    data class UpdateChatroomCache(val chatroomUpdateFields: Map<String, String?>) : ChatroomIntent()

    data class TogglePushNotificationForChatroomAuthUser(val send: Boolean) : ChatroomIntent()

    object DeleteChatroom : ChatroomIntent()
}