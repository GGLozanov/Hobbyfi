package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.User
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

// TODO: This is a separate viewmodelfactory from MainActivityVMFactory because it will 99% have more parameters in the future
class ChatroomActivityViewModelFactory(val application: Application, val _isFacebookAuthUser: Boolean, val user: User?)
    : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ChatroomActivityViewModel::class.java)) {
            return ChatroomActivityViewModelFactory(application, _isFacebookAuthUser, user) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}