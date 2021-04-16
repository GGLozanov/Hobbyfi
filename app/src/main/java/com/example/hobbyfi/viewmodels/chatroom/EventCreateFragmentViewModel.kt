package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventCreateFragmentViewModel(
    application: Application
) : EventAccessorViewModel(application), Base64ImageHolder by Base64ImageHolderViewModel() {
    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventIntent.CreateEvent -> {
                        viewModelScope.launch { // again, potential img upload, blah blah
                            createEvent(it.chatroomId)
                        }
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun createEvent(chatroomId: Long) {
        mainStateIntent.setState(EventState.Loading)

        if(_eventDate == null || eventLatLng == null) {
            mainStateIntent.setState(EventState.Error(
                Constants.invalidEventInfoError
            ))
            return
        }

        mainStateIntent.setState(try {
            val parsedEventDate = Constants.dateTimeFormatter.format(_eventDate!!)
            val response = eventRepository.createEvent(
                name.value!!,
                description.value,
                parsedEventDate,
                eventLatLng!!.latitude,
                eventLatLng!!.longitude,
                chatroomId
            )

            val event = Event(
                response!!.id,
                name.value!!,
                description.value,
                response.startDate,
                parsedEventDate,
                if(base64Image.originalUri != null) BuildConfig.BASE_URL + "uploads/" + Constants.eventProfileImageDir(response.id)
                        + "/" + response.id + ".jpg" else null,
                eventLatLng!!.latitude,
                eventLatLng!!.longitude,
                chatroomId
            )

            EventState.OnData.EventCreateResult(event)
        } catch(ex: Exception) {
            EventState.Error(
                ex.message,
                ex.isCritical
            )
        })
    }

    init {
        handleIntent()
    }
}