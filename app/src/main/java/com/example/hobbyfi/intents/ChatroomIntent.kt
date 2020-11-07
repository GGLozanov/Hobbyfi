package com.example.hobbyfi.intents

// fire off request for chatroom info upon entering mainActivity
// encapsulates chatroom tag fetching (chatroom info fetch intent)
// includes delete/edit chatroom intents
// and includes instant delete chatroom intent
sealed class ChatroomIntent : Intent {
    sealed class FetchChatrooms : ChatroomIntent() // pass in page number for pagination
    sealed class FetchChatroom : ChatroomIntent() // fetch registered user's chatroom => pass in chatroom id?

    sealed class UpdateChatroom(val chatroomId: Int, val chatroomUpdateFields: Map<String, String>) : ChatroomIntent()
    sealed class DeleteChatroom(val chatroomId: Int) : ChatroomIntent()
}