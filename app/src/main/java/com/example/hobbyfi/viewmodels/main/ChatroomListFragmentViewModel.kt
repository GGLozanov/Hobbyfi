package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragmentViewModel(application: Application) : StateIntentViewModel<ChatroomListState, ChatroomListIntent>(application) {
    // TODO: Upon fetching pagingdata, set the ChatroomState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance
    init {
        handleIntent()
    }

    private val chatroomRepository: ChatroomRepository by instance("chatroomRepository")

    override val _mainState: MutableStateFlow<ChatroomListState> = MutableStateFlow(ChatroomListState.Idle)

    private var _chatroomPagingData: PagingData<Chatroom>? = null
    val chatroomPagingData: PagingData<Chatroom>? get() = _chatroomPagingData

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collectLatest {
                when(it) {
                    is ChatroomListIntent.FetchChatrooms -> {
                        fetchChatrooms(it.shouldDisplayAuthChatroom)
                    }
                }
            }
        }
    }

    private suspend fun fetchChatrooms(shouldDisplayAuthChatroom: Boolean) {
        _mainState.value = ChatroomListState.Loading
        chatroomRepository.getChatrooms(shouldFetchAuthChatroom = shouldDisplayAuthChatroom).catch { e ->
            e.printStackTrace()
            _mainState.value = if(e is Repository.ReauthenticationException)
                ChatroomListState.Error(Constants.reauthError, shouldReauth = true) else ChatroomListState.Error(e.message)
        }.collect {
            _chatroomPagingData = it
            _mainState.value = ChatroomListState.ChatroomsResult(
                it
            )
        }
    }
}