package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.viewmodels.chatroom.EventDetailsFragmentViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventEditDialogFragmentViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class EventViewModelFactory(val application: Application, private val event: Event) : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(EventMapsActivityViewModel::class.java) -> {
                EventMapsActivityViewModel(application, event) as T
            }
            modelClass.isAssignableFrom(EventEditDialogFragmentViewModel::class.java) -> {
                EventEditDialogFragmentViewModel(application, event) as T
            }
            modelClass.isAssignableFrom(EventDetailsFragmentViewModel::class.java) -> {
                EventDetailsFragmentViewModel(application, event) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
}