package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class EventCreateFragmentViewModel(application: Application)
    : StateIntentViewModel<EventState, EventIntent>(application), NameDescriptionBindable by NameDescriptionBindableViewModel() {

    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    override val mainStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is EventIntent.CreateEvent -> {
                        createEvent()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private fun createEvent() {
        mainStateIntent.setState(EventState.Loading)
    }

}