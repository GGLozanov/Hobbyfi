package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class AuthUserChatroomViewModelFactory(val application: Application, val user: User?, val chatroom: Chatroom?) : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ChatroomActivityViewModel::class.java)) {
            return ChatroomActivityViewModel(application, user, chatroom) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}