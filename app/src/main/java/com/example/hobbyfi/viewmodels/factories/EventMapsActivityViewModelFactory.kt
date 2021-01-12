package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.User
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class EventMapsActivityViewModelFactory(val application: Application, private val event: Event) : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(EventMapsActivityViewModel::class.java)) {
            return EventMapsActivityViewModel(application, event) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}