package com.example.hobbyfi.intents

sealed class ChatroomListIntent : Intent {
    object FetchChatrooms : ChatroomListIntent() // pass in page number for pagination?
    object FetchJoinedChatrooms : ChatroomListIntent()
}
