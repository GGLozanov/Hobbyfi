package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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