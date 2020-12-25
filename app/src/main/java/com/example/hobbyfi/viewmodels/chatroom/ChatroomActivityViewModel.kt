package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import androidx.paging.PagingData
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.models.User
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.UserListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(application: Application, user: User?, chatroom: Chatroom?)
    : AuthChatroomHolderViewModel(application, user, chatroom) {

    private var currentUsers: Flow<PagingData<User>>? = null

    private var _currentAdapterUsers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    val currentAdapterUsers: LiveData<List<User>> get() = _currentAdapterUsers

    private val eventStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }

    private val userStateIntent: StateIntent<UserListState, UserListIntent> = object : StateIntent<UserListState, UserListIntent>() {
        override val _state: MutableStateFlow<UserListState> = MutableStateFlow(UserListState.Idle)
    }

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            eventStateIntent.intentAsFlow().collectLatest {

            }
        }
        viewModelScope.launch {
            userStateIntent.intentAsFlow().collectLatest {

            }
        }
    }

    init {
        handleIntent()
    }

    private fun fetchUsers() {

    }

    // TODO: Hide behind intent? Also, bruh conversions
    fun setCurrentUsers(users: List<User>) {
        val mUsers = users.toMutableList()
        if(authUser.value != null && !mUsers.contains(authUser.value)) {
            mUsers.add(authUser.value!!)
        }
        _currentAdapterUsers.value = mUsers

    }
}