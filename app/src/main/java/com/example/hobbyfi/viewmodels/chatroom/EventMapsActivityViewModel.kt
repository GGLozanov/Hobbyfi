package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class EventMapsActivityViewModel(application: Application, private val eventId: Long) :
        StateIntentViewModel<EventListState, EventListIntent>(application) {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    override val mainStateIntent: StateIntent<EventListState, EventListIntent> = object : StateIntent<EventListState, EventListIntent>() {
        override val _state: MutableStateFlow<EventListState> = MutableStateFlow(EventListState.Idle)
    }

    init {
        handleIntent()
    }

    suspend fun sendEventIntent(intent: EventListIntent) = mainStateIntent.sendIntent(intent)

    override fun handleIntent() {

    }
}