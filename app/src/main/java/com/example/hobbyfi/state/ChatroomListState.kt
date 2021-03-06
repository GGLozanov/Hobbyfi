package com.example.hobbyfi.state

import androidx.paging.PagingData
import com.example.hobbyfi.models.data.Chatroom
import kotlinx.coroutines.flow.Flow

sealed class ChatroomListState : State {
    object Idle : ChatroomListState()
    object Loading : ChatroomListState()

    sealed class OnData : ChatroomListState() {
        data class ChatroomsResult(val chatrooms: Flow<PagingData<Chatroom>>) : ChatroomListState()
        data class JoinedChatroomsResult(val joinedChatrooms: Flow<PagingData<Chatroom>>) : ChatroomListState()
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : ChatroomListState()
}