package com.example.hobbyfi.viewmodels.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragmentViewModel(application: Application) : StateIntentViewModel<ChatroomListState, ChatroomListIntent>(application) {
    private val chatroomRepository: ChatroomRepository by instance("chatroomRepository")

    override val mainStateIntent: StateIntent<ChatroomListState, ChatroomListIntent> = object : StateIntent<ChatroomListState, ChatroomListIntent>() {
        override val _state: MutableStateFlow<ChatroomListState> = MutableStateFlow(ChatroomListState.Idle)
    }

    init {
        handleIntent()
    }

    private var _currentChatrooms: Flow<PagingData<Chatroom>>? = null
    private var _currentJoinedChatrooms: Flow<PagingData<Chatroom>>? = null

    val currentChatrooms: Flow<PagingData<Chatroom>>? get() = _currentChatrooms
    val currentJoinedChatrooms: Flow<PagingData<Chatroom>>? get() = _currentJoinedChatrooms

    private var _buttonSelectedChatroom: Chatroom? = null
    val buttonSelectedChatroom: Chatroom? get() = _buttonSelectedChatroom

    fun setButtonSelectedChatroom(chatroom: Chatroom?) {
        _buttonSelectedChatroom = chatroom
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is ChatroomListIntent.FetchChatrooms -> {
                        Log.i("ChatroomListFragmentVM", "Handling FetchChatrooms intent")
                        fetchChatrooms()
                    }
                    is ChatroomListIntent.FetchJoinedChatrooms -> {
                        Log.i("ChatroomListFragmentVM", "Handling FetchJoinedChatrooms intent")
                        fetchJoinedChatrooms()
                    }
                }
            }
        }
    }

    private suspend fun fetchChatrooms() {
        mainStateIntent.setState(ChatroomListState.Loading)

        Log.i("ChatroomListFragmentVM", "Current chatrooms: ${_currentChatrooms}")
        if(_currentChatrooms == null) {
            _currentChatrooms = chatroomRepository.getChatrooms()
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(ChatroomListState.OnData.ChatroomsResult(_currentChatrooms!!))
    }

    private suspend fun fetchJoinedChatrooms() {
        mainStateIntent.setState(ChatroomListState.Loading)

        Log.i("ChatroomListFragmentVM", "Current JOINED chatrooms: ${_currentJoinedChatrooms}")
        if(_currentJoinedChatrooms == null) {
            _currentJoinedChatrooms = chatroomRepository.getAuthChatrooms()
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(ChatroomListState.OnData.JoinedChatroomsResult(_currentJoinedChatrooms!!))
    }
}