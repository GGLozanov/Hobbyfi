package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.state.UserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalCoroutinesApi
abstract class AuthChatroomHolderViewModel(application: Application) : AuthUserHolderViewModel(application) {
    protected var _authChatroom : Chatroom? = null

    protected val _chatroomState : MutableStateFlow<ChatroomState>
            = MutableStateFlow(ChatroomState.Idle)
    val chatroomState: StateFlow<ChatroomState> get() = _chatroomState

    protected val chatroomIntent: Channel<ChatroomIntent> = Channel(Channel.UNLIMITED)

    override fun handleIntent() {
        super.handleIntent()
    }

    suspend fun sendChatroomIntent(i: ChatroomIntent) {
        chatroomIntent.send(i)
    }
}