package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.models.User
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(application: Application, _isFacebookAuthUser: Boolean, user: User?)
    : AuthChatroomHolderViewModel(application, _isFacebookAuthUser, user) {
}