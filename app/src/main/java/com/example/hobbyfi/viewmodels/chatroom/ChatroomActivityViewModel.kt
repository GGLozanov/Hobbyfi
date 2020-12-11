package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.models.User
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(application: Application, user: User?, chatroom: Chatroom?)
    : AuthChatroomHolderViewModel(application, user, chatroom) {

    init {
        handleIntent()
    }
}