package com.example.hobbyfi.viewmodels.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var currentChatrooms: Flow<PagingData<Chatroom>>? = null
    private var currentJoinedChatrooms: Flow<PagingData<Chatroom>>? = null

    private var _buttonSelectedChatroom: Chatroom? = null
    val buttonSelectedChatroom: Chatroom? get() = _buttonSelectedChatroom

    fun setButtonSelectedChatroom(chatroom: Chatroom?) {
        _buttonSelectedChatroom = chatroom
    }

    fun setCurrentChatrooms(chatrooms: Flow<PagingData<Chatroom>>?) {
        currentChatrooms = chatrooms
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is ChatroomListIntent.FetchChatrooms -> {
                        Log.i("ChatroomListFragmentVM", "Handling FetchChatrooms intent")
                        fetchChatrooms(it.userChatroomIds)
                    }
                    is ChatroomListIntent.FetchJoinedChatrooms -> {
                        Log.i("ChatroomListFragmentVM", "Handling FetchJoinedChatrooms intent")
                        fetchJoinedChatrooms(it.userChatroomIds)
                    }
                }
            }
        }
    }

    private fun fetchChatrooms(userChatroomIds: List<Long>?) {
        mainStateIntent.setState(ChatroomListState.Loading)

        Log.i("ChatroomListFragmentVM", "Current chatrooms: ${currentChatrooms}")
        if(currentChatrooms == null) {
            currentChatrooms = chatroomRepository.getChatrooms(userChatroomIds = userChatroomIds)
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(ChatroomListState.OnData.ChatroomsResult(currentChatrooms!!))
    }

    private fun fetchJoinedChatrooms(userChatroomIds: List<Long>?) {
        mainStateIntent.setState(ChatroomListState.Loading)

        Log.i("ChatroomListFragmentVM", "Current JOINED chatrooms: ${currentJoinedChatrooms}")
        if(currentJoinedChatrooms == null) {
            currentJoinedChatrooms = chatroomRepository.getAuthChatrooms(userChatroomIds = userChatroomIds)
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(ChatroomListState.OnData.ChatroomsResult(currentJoinedChatrooms!!))
    }
}