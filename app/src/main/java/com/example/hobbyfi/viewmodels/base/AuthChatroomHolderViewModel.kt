package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.Intent
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
abstract class AuthChatroomHolderViewModel(application: Application, user: User?, chatroom: Chatroom?)
    : AuthUserHolderViewModel(application, user) {

    protected var _authChatroom: MutableLiveData<Chatroom?> = MutableLiveData(chatroom)
    val authChatroom: LiveData<Chatroom?> get() = _authChatroom

    protected val chatroomRepository: ChatroomRepository by instance(tag = "chatroomRepository")

    protected val _chatroomState: MutableStateFlow<ChatroomState>
            = MutableStateFlow(ChatroomState.Idle)
    val chatroomState: StateFlow<ChatroomState> get() = _chatroomState

    protected val chatroomIntent: Channel<ChatroomIntent> = Channel(Channel.UNLIMITED)

    private var _isAuthUserChatroomOwner = MutableLiveData(authUser.value?.id ==
            authChatroom.value?.ownerId) // initial check; updated every time auth user or auth chatroom changes
    val isAuthUserChatroomOwner get() = _isAuthUserChatroomOwner

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            chatroomIntent.consumeAsFlow().collect {
                when(it) {
                    is ChatroomIntent.FetchChatroom -> {
                        fetchChatroom()
                    }
                    is ChatroomIntent.DeleteChatroom -> {

                    }
                    is ChatroomIntent.UpdateChatroom -> {

                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    fun setChatroom(chatroom: Chatroom) {
        _authChatroom.value = chatroom
        _isAuthUserChatroomOwner.value = isAuthUserAuthChatroomOwner()
    }

    suspend fun sendChatroomIntent(i: ChatroomIntent) {
        chatroomIntent.send(i)
    }

    override fun setUser(user: User) {
        super.setUser(user)
        _isAuthUserChatroomOwner.value = isAuthUserAuthChatroomOwner()
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

    private suspend fun deleteChatroom() {

    }

    private suspend fun updateChatroom() {

    }

    private fun isAuthUserAuthChatroomOwner(): Boolean {
        // evaluates current auth room and auth user ownership
        // (for when user and chatroom aren't passed and need to be fetched async - i.e. deeplink)
        return authUser.value?.id ==
                authChatroom.value?.ownerId
    }
}