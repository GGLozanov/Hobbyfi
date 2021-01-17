package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class EventMapsActivityViewModel(
    application: Application,
    initialEvent: Event
) : StateIntentViewModel<EventListState, EventListIntent>(application) {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    private val _event: MutableLiveData<Event> = MutableLiveData(initialEvent)
    val event: LiveData<Event> get() = _event

    override val mainStateIntent: StateIntent<EventListState, EventListIntent> = object : StateIntent<EventListState, EventListIntent>() {
        override val _state: MutableStateFlow<EventListState> = MutableStateFlow(EventListState.Idle)
    }

    private val userGeoPointStateIntent: StateIntent<UserGeoPointState, UserGeoPointIntent> = object : StateIntent<UserGeoPointState, UserGeoPointIntent>() {
        override val _state: MutableStateFlow<UserGeoPointState> = MutableStateFlow(UserGeoPointState.Idle)
    }

    val userGeoPointState: StateFlow<UserGeoPointState>
        get() = userGeoPointStateIntent.state

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
                    is EventListIntent.DeleteEventsCache -> { // i.e. delete old events from notification
                        deleteEventsCache(it.eventIds)
                    }
                    is EventListIntent.DeleteAnEventCache -> {
                        deleteEventCache(it.eventId)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun deleteEventsCache(eventIds: List<Long>) {
        mainStateIntent.setState(if(eventRepository.deleteEventsCache(eventIds))
            EventListState.OnData.DeleteEventsCacheResult(eventIds)
        else EventListState.Error(Constants.cacheDeletionError, true)) // shouldReath = shouldExit here
    }

    private suspend fun deleteEventCache(eventId: Long) {
        mainStateIntent.setState(if(eventRepository.deleteEventCache(eventId))
            EventListState.OnData.DeleteAnEventCacheResult(eventId)
        else EventListState.Error(Constants.cacheDeletionError, true))
    }

    private suspend fun updateAndSaveCurrentEvent(updateFields: Map<String?, String?>) {
        val updatedEvent = _event.value!!.updateFromFieldMap(updateFields)
        eventRepository.saveEvent(updatedEvent)
        _event.value = updatedEvent
    }
}