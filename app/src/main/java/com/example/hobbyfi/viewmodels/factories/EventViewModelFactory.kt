package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.User
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventDetailsViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventEditFragmentViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class EventViewModelFactory(val application: Application, private val event: Event) : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(EventMapsActivityViewModel::class.java) -> {
                EventMapsActivityViewModel(application, event) as T
            }
            modelClass.isAssignableFrom(EventEditFragmentViewModel::class.java) -> {
                EventEditFragmentViewModel(application, event) as T
            }
            modelClass.isAssignableFrom(EventDetailsViewModel::class.java) -> {
                EventDetailsViewModel(application, event) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
}