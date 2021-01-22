package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.ViewModel
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class EventDetailsViewModel(application: Application, private val event: Event) : BaseViewModel(application) {
    // TODO: Implement the ViewModel

    // TODO: Fetch other users & user geopoint here (?)

    val eventName get() = event.name
}