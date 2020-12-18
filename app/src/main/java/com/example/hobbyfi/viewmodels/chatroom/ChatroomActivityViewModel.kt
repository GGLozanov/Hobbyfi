package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.models.User
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.UserListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(application: Application, user: User?, chatroom: Chatroom?)
    : AuthChatroomHolderViewModel(application, user, chatroom) {

    // BIG TODO: Fix code dup around viewmodels with multiple states/intents by bundling these 3 properties into an object
    // Composition ftw...!
    private val _eventState: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    val eventState: StateFlow<EventState> get() = _eventState
    private val eventIntent: Channel<EventIntent> = Channel(Channel.UNLIMITED)


    private val _userState: MutableStateFlow<UserListState> = MutableStateFlow(UserListState.Idle)
    val userState: StateFlow<UserListState> get() = _userState
    private val userIntent: Channel<UserListIntent> = Channel(Channel.UNLIMITED)

    init {
        handleIntent()
    }

    override fun handleIntent() {
        super.handleIntent()

    }
}