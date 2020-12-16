package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.ChatroomEditFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class AuthChatroomViewModelFactory(val application: Application, val chatroom: Chatroom?)
    : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ChatroomEditFragmentViewModel::class.java)) {
            return ChatroomEditFragmentViewModel(application, chatroom) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}