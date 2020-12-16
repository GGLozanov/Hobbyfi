package com.example.hobbyfi.viewmodels.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private var currentChatrooms: Flow<PagingData<Chatroom>>? = null
    private var _hasDeletedCacheForSession = false
    val hasDeletedCacheForSession get() = _hasDeletedCacheForSession
    private var _buttonSelectedChatroom: Chatroom? = null
    val buttonSelectedChatroom: Chatroom? get() = _buttonSelectedChatroom

    fun setHasDeletedCacheForSession(hasDeletedCache: Boolean) {
        _hasDeletedCacheForSession = hasDeletedCache
    }

    fun setButtonSelectedChatroom(chatroom: Chatroom?) {
        _buttonSelectedChatroom = chatroom
    }

    fun setCurrentChatrooms(chatrooms: Flow<PagingData<Chatroom>>?) {
        currentChatrooms = chatrooms
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collectLatest {
                when(it) {
                    is ChatroomListIntent.FetchChatrooms -> {
                        Log.i("ChatroomListFragmentVM", "Handling FetchChatrooms intent with shouldDisplayAuthChatroom: ${it.shouldDisplayAuthChatroom}")
                        fetchChatrooms(it.shouldDisplayAuthChatroom)
                    }
                    is ChatroomListIntent.DeleteChatroomsCache -> {
                        Log.i("ChatroomListFragmentVM", "Handling DeleteChatroomsCache intent with auth chatroom id: ${it.authChatroomId}")
                        deleteChatroomsCache(it.authChatroomId)
                    }
                }
            }
        }
    }

    private fun fetchChatrooms(shouldDisplayAuthChatroom: Boolean) {
        _mainState.value = ChatroomListState.Loading

        Log.i("ChatroomListFragmentVM", "Current chatrooms: ${currentChatrooms}")
        if(currentChatrooms == null) {
            currentChatrooms = chatroomRepository.getChatrooms(shouldFetchAuthChatroom = shouldDisplayAuthChatroom)
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        _mainState.value = ChatroomListState.ChatroomsResult(currentChatrooms!!, shouldDisplayAuthChatroom)
    }

    private suspend fun deleteChatroomsCache(authChatroomId: Long) {
        var state: ChatroomListState = ChatroomListState.Error(Constants.cacheDeletionError)

        // deletes other cached chatrooms (not auth'd) for user
        if(viewModelScope.async {
                chatroomRepository.deleteChatrooms(authChatroomId)
            }.await()) {
            _hasDeletedCacheForSession = true
            state = ChatroomListState.DeleteChatroomsCacheResult
        }

        _mainState.value = state
    }
}