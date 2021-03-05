package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindable
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindableViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.generic.instance
import java.util.*

@ExperimentalCoroutinesApi
abstract class EventAccessorViewModel(
    application: Application
) : StateIntentViewModel<EventState, EventIntent>(application), NameDescriptionBindable by NameDescriptionBindableViewModel() {
    protected val eventRepository: EventRepository by instance(tag = "eventRepository")

    override val mainStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }

    protected var _eventDate: Date? = null
    val eventDate get() = _eventDate
    var eventLatLng: LatLng? = null

    @JvmName("setEventDate1")
    fun setEventDate(date: Date?) {
        _eventDate = date
    }
}