package com.example.hobbyfi.intents

sealed class ChatroomListIntent : Intent {
    data class FetchChatrooms(val shouldDisplayAuthChatroom: Boolean) : ChatroomListIntent() // pass in page number for pagination?
}
