package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(application: Application) : AuthChatroomHolderViewModel(application) {
}