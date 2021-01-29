package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.replaceOrAdd
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.UserGeoPointState
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Exception

@ExperimentalCoroutinesApi
class EventMapsActivityViewModel(
    application: Application,
    initialEvent: Event
) : UserGeoPointAccessorViewModel(application, initialEvent) {

    private val _event: MutableLiveData<Event> = MutableLiveData(_relatedEvent)
    val event: LiveData<Event> get() = _event

    private var _userMarkers: List<Marker>? = null
    val userMarkers: List<Marker>? get() = _userMarkers

    fun setUserMarkers(markers: List<Marker>?) {
        _userMarkers = markers
    }

    private val eventsStateIntent: StateIntent<EventListState, EventListIntent> = object : StateIntent<EventListState, EventListIntent>() {
        override val _state: MutableStateFlow<EventListState> =
            MutableStateFlow(EventListState.Idle)
    }
    val eventsState: StateFlow<EventListState>
        get() = eventsStateIntent.state

    suspend fun sendEventsIntent(intent: EventListIntent) = eventsStateIntent.sendIntent(intent)

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            eventsStateIntent.intentAsFlow().collectLatest {
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
        eventsStateIntent.setState(if(eventRepository.deleteEventsCache(eventIds))
            EventListState.OnData.DeleteEventsCacheResult(eventIds)
        else EventListState.Error(Constants.cacheDeletionError, true)) // shouldReath = shouldExit here
    }

    private suspend fun deleteEventCache(eventId: Long) {
        eventsStateIntent.setState(if(eventRepository.deleteEventCache(eventId))
            EventListState.OnData.DeleteAnEventCacheResult(eventId)
        else EventListState.Error(Constants.cacheDeletionError, true))
    }

    private suspend fun updateAndSaveCurrentEvent(updateFields: Map<String?, String?>) {
        val updatedEvent = _event.value!!.updateFromFieldMap(updateFields)
        eventRepository.saveEvent(updatedEvent)
        _event.value = updatedEvent
    }

    fun updateNewGeoPointInList(geoPoint: UserGeoPoint) {
        _userGeoPoints?.value = _userGeoPoints?.value?.replaceOrAdd(
            geoPoint, { gp -> gp.username == geoPoint.username })
    }

    private var _lastReceivedLocation: LatLng? = null
    val lastReceivedLocation get() = _lastReceivedLocation

    fun setLastReceivedLocation(loc: LatLng?) {
        _lastReceivedLocation = loc
    }

    private var _initialStart: Boolean = true
    val initialStart get() = _initialStart

    fun setInitialStart(initialStart: Boolean) {
        _initialStart = initialStart
    }

    // little bruh hack; w/e
    fun forceEventObservation() {
        _event.value = _event.value
    }

    init {
        handleIntent()
    }
}