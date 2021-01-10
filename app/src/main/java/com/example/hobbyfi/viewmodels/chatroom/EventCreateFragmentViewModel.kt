package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Base64Image
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import java.util.*

@ExperimentalCoroutinesApi
class EventCreateFragmentViewModel(
    application: Application
) : StateIntentViewModel<EventState, EventIntent>(application), NameDescriptionBindable by NameDescriptionBindableViewModel() {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    override val mainStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }

    private var eventDate: Date? = null
    var eventLatLng: LatLng? = null
    
    fun setEventDate(date: Date?) {
        eventDate = date
    }

    val base64Image: Base64Image = Base64Image()

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is EventIntent.CreateEvent -> {
                        createEvent(it.chatroomId)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun createEvent(chatroomId: Long) {
        mainStateIntent.setState(EventState.Loading)

        if(eventDate == null || eventLatLng == null) {
            mainStateIntent.setState(EventState.Error(
                Constants.invalidEventInfoError
            ))
            return
        }

        mainStateIntent.setState(try {
            val state = EventState.OnData.EventCreateResult(eventRepository.createEvent(
                name.value!!,
                description.value,
                eventDate.toString(),
                base64Image.base64,
                eventLatLng!!.latitude,
                eventLatLng!!.longitude
            ))

            val event = Event(
                state.response!!.id,
                name.value!!,
                description.value,
                state.response.startDate,
                eventDate.toString(),
                if(base64Image.base64 != null) BuildConfig.BASE_URL + "uploads/" + Constants.eventProfileImageDir(state.response.id)
                        + "/" + state.response.id + ".jpg" else null,
                eventLatLng!!.latitude,
                eventLatLng!!.longitude,
                chatroomId
            )

            eventRepository.saveEvent(event)

            state
        } catch(ex: Exception) {
            EventState.Error(
                ex.message,
                ex.isCritical
            )
        })
    }
}