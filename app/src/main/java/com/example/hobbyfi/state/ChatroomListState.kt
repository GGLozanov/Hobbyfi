package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.Chatroom
import kotlinx.coroutines.flow.Flow

sealed class ChatroomListState : State {
    object Idle : ChatroomListState()
    object Loading : ChatroomListState()

    data class ChatroomsResult(val chatrooms: Flow<PagingData<Chatroom>>, val isJustAuthChatroom: Boolean = false) : ChatroomListState()  // TODO: if fetch from network the map<String, Chatroom> will be converted to a flat list and processed by pagination

    object DeleteChatroomsCacheResult : ChatroomListState()

    data class Error(val error: String?, val shouldReauth: Boolean = false) : ChatroomListState()
}