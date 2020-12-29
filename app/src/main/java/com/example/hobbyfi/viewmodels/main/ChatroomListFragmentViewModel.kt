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
import com.example.hobbyfi.shared.Constants
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
class ChatroomListFragmentViewModel(application: Application) :
    StateIntentViewModel<ChatroomListState, ChatroomListIntent>(application) {
    private val chatroomRepository: ChatroomRepository by instance("chatroomRepository")

    override val mainStateIntent: StateIntent<ChatroomListState, ChatroomListIntent> = object : StateIntent<ChatroomListState, ChatroomListIntent>() {
        override val _state: MutableStateFlow<ChatroomListState> = MutableStateFlow(ChatroomListState.Idle)
    }

    init {
        handleIntent()
    }

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
            mainStateIntent.intentAsFlow().collectLatest {
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
        mainStateIntent.setState(ChatroomListState.Loading)

        Log.i("ChatroomListFragmentVM", "Current chatrooms: ${currentChatrooms}")
        if(currentChatrooms == null) {
            currentChatrooms = chatroomRepository.getChatrooms(shouldFetchAuthChatroom = shouldDisplayAuthChatroom)
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(ChatroomListState.OnData.ChatroomsResult(currentChatrooms!!, shouldDisplayAuthChatroom))
    }

    private suspend fun deleteChatroomsCache(authChatroomId: Long) {

        // deletes other cached chatrooms (not auth'd) for user
        withContext(viewModelScope.coroutineContext) {
            chatroomRepository.deleteChatrooms(authChatroomId) // ignore result for now because, c'mon, where could it go wrong?
            _hasDeletedCacheForSession = true
            mainStateIntent.setState(ChatroomListState.OnData.DeleteChatroomsCacheResult)
        }
    }
}