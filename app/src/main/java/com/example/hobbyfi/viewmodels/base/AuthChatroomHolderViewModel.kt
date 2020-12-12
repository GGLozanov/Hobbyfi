package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.shared.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthChatroomHolderViewModel(application: Application, user: User?, protected val chatroom: Chatroom?)
    : AuthUserHolderViewModel(application, user) {
    init {
        // TODO: Save chatroom here
        if(chatroom != null) {

        }
    }

    protected val chatroomRepository: ChatroomRepository by instance(tag = "chatroomRepository")

    protected val _chatroomState: MutableStateFlow<ChatroomState>
            = MutableStateFlow(ChatroomState.Idle)
    val chatroomState: StateFlow<ChatroomState> get() = _chatroomState

    protected val chatroomIntent: Channel<ChatroomIntent> = Channel(Channel.UNLIMITED)

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            chatroomIntent.consumeAsFlow().collect {

            }
        }
    }

    suspend fun sendChatroomIntent(i: ChatroomIntent) {
        chatroomIntent.send(i)
    }

    private suspend fun fetchChatroom() {
        // TODO: If this doesn't work or seems too coupled, make a separate fetch chatroom method and add it to ChatroomState/ChatroomIntent
        _chatroomState.value = ChatroomState.Loading

        chatroomRepository.getChatroom().catch { e ->
            e.printStackTrace()
            _chatroomState.value = if(e is Repository.ReauthenticationException)
                ChatroomState.Error(Constants.reauthError, shouldReauth = true) else ChatroomState.Error(e.message)
        }.collect {
            if(it != null) {
                _chatroomState.value = ChatroomState.OnData.ChatroomResult(it)
            }
        }
    }
}