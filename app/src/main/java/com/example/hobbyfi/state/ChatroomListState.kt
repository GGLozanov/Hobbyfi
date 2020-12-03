package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.Chatroom

sealed class ChatroomListState : State {
    object Idle : ChatroomListState()
    object Loading : ChatroomListState()

    data class ChatroomsResult(val chatrooms: PagingData<Chatroom>) : ChatroomListState()  // TODO: if fetch from network the map<String, Chatroom> will be converted to a flat list and processed by pagination

    data class Error(val error: String?, val shouldReauth: Boolean = false) : ChatroomListState()
}