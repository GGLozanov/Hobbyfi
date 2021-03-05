package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import com.example.hobbyfi.models.data.Event
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class EventDetailsFragmentViewModel(
    application: Application,
    event: Event
) : UserGeoPointAccessorViewModel(application, event) {

    private var _lastMarker: Marker? = null
    private val lastMarker: Marker? get() = _lastMarker

    fun removeAndSetLastMarker(marker: Marker?) {
        _lastMarker?.remove()
        _lastMarker = marker
    }

    fun setEvent(ev: Event) {
        this._relatedEvent = ev
    }

    private var _invokedJoinEventButton: Boolean = false
    val invokedJoinEventButton get() = _invokedJoinEventButton

    fun setInvokedJoinEventButton(invoked: Boolean) {
        _invokedJoinEventButton = invoked
    }

    init {
        handleIntent()
    }
}