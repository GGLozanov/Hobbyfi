package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application

import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
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
                        createEvent(it.chatroomId)
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
                getApplication<MainApplication>().applicationContext.getString(
                    R.string.invalid_event_info_error)
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

            EventState.OnData.EventCreateResult(Event(
                response!!.id,
                name.value!!,
                description.value,
                response.startDate,
                parsedEventDate,
                null,
                eventLatLng!!.latitude,
                eventLatLng!!.longitude,
                chatroomId
            )) // image uploaded through WorkManager on success
        } catch(ex: CancellationException) {
            throw ex // swallow cancellation exceptions (request is performed successfully most of the time either way)
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