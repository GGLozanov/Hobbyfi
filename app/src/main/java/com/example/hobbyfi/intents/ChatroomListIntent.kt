package com.example.hobbyfi.intents

sealed class ChatroomListIntent : Intent {
    data class FetchChatrooms(val userChatroomIds: List<Long>?) : ChatroomListIntent() // pass in page number for pagination?
    data class FetchJoinedChatrooms(val userChatroomIds: List<Long>?) : ChatroomListIntent()
}
