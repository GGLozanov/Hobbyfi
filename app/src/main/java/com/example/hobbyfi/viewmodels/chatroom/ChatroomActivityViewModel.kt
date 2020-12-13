package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.models.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(application: Application, user: User?, chatroom: Chatroom?)
    : AuthChatroomHolderViewModel(application, user, chatroom) {

    init {
        handleIntent()
    }
}