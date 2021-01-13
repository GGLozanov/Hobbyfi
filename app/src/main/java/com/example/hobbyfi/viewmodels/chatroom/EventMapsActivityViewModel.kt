package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class EventMapsActivityViewModel(application: Application, initialEvent: Event) :
        StateIntentViewModel<EventListState, EventListIntent>(application) {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    private val _event: MutableLiveData<Event> = MutableLiveData(initialEvent)
    val event: LiveData<Event> get() = _event

    override val mainStateIntent: StateIntent<EventListState, EventListIntent> = object : StateIntent<EventListState, EventListIntent>() {
        override val _state: MutableStateFlow<EventListState> = MutableStateFlow(EventListState.Idle)
    }

    init {
        handleIntent()
    }

    suspend fun sendEventsIntent(intent: EventListIntent) = mainStateIntent.sendIntent(intent)

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is EventListIntent.UpdateAnEventCache -> {
                        updateAndSaveCurrentEvent(it.eventUpdateFields)
                    }
                    is EventListIntent.DeleteEventsCache -> {
                        eventRepository.deleteEventsCache(it.eventIds)
                    }
                    is EventListIntent.DeleteAnEventCache -> {
                        eventRepository.deleteEventCache(it.eventId)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun updateAndSaveCurrentEvent(updateFields: Map<String?, String?>) {
        val updatedEvent = _event.value!!.updateFromFieldMap(updateFields)
        eventRepository.saveEvent(updatedEvent)
        _event.value = updatedEvent
    }
}