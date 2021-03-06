package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.models.data.UserGeoPoint
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.forceObserve
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.shared.replaceOrAdd
import com.example.hobbyfi.state.EventListState
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    // FIXME: Code dup w/ ChatroomActivityViewModel (can't be abstractd w/ interface though 'cause encapsulation?)
    private var _shownSocketError: Boolean = false
    val shownSocketError: Boolean get() = _shownSocketError
    fun setShownSocketError(shown: Boolean) {
        _shownSocketError = shown
    }

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
                    is EventListIntent.RefetchEvent -> {
                        refetchEvent()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    // only triggers LD observers (kind of a "silent" StateIntent combo)
    private suspend fun refetchEvent() {
        eventRepository.getEvent(event.value!!.id).catch { e ->
            e.printStackTrace()
            eventsStateIntent.setState(
                EventListState.Error(
                    e.message,
                    e.isCritical
                )
            )
        }.collectLatest {
            if(it != null) {
                setEvent(it)
            } else {
                Log.w("EventMapsActivityVM", "Event repository returned null event on refetchEvent() call!")
            }
        }
    }

    private fun setEvent(event: Event) {
        _relatedEvent = event
        _event.value = event
    }

    private suspend fun deleteEventsCache(eventIds: List<Long>) {
        eventsStateIntent.setState(EventListState.Loading)
        eventsStateIntent.setState(if(eventRepository.deleteEventsCache(eventIds))
            EventListState.OnData.DeleteEventsCacheResult(eventIds)
        else EventListState.Error(getApplication<MainApplication>()
            .applicationContext.getString(R.string.cache_deletion_error), true)) // shouldReath = shouldExit here
    }

    private suspend fun deleteEventCache(eventId: Long) {
        eventsStateIntent.setState(EventListState.Loading)
        eventsStateIntent.setState(if(eventRepository.deleteEventCache(eventId))
            EventListState.OnData.DeleteAnEventCacheResult(eventId)
        else EventListState.Error(getApplication<MainApplication>()
            .applicationContext.getString(R.string.cache_deletion_error), true))
    }

    private suspend fun updateAndSaveCurrentEvent(updateFields: Map<String, String?>) {
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
        _event.forceObserve()
    }

    init {
        handleIntent()
    }
}