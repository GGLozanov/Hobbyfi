package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindable
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindableViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class EventEditFragmentViewModel(application: Application, private val event: Event) : StateIntentViewModel<EventState, EventIntent>(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {

    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    override val mainStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventIntent.DeleteEvent -> {
                        deleteEvent()
                    }
                    is EventIntent.UpdateEvent -> {
                        updateEvent(it.eventUpdateFields)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }


    private suspend fun updateEvent(updateFields: Map<String?, String?>) {
        mainStateIntent.setState(EventState.Loading)

        mainStateIntent.setState(try {
            val state = EventState.OnData.EventEditResult(eventRepository.editEvent(
                updateFields
            ))

            // TODO: This update should trigger FCM message to update events list cache in backing activity on update
            // eventRepository.saveEvent(updateFields)

            state
        } catch(ex: Exception) {
            ex.printStackTrace()

            EventState.Error(
                ex.message,
                ex.isCritical
            )
        })
    }

    private suspend fun deleteEvent() {
        mainStateIntent.setState(EventState.Loading)

        mainStateIntent.setState(try {
            val eventId = event.id

            val state = EventState.OnData.EventDeleteResult(
                eventRepository.deleteEvent(eventId)
            )

            // TODO: This delete should trigger FCM message to delete event in list cache from backing activity on delete
            // deleteEventCache(eventId)

            state
        } catch(ex: Exception) {
            ex.printStackTrace()

            EventState.Error(
                ex.message
            )
        })
    }

    // FIXME: Ge. Ne. RIIIIICS. Well, not really but still code dup with other deleteCache methods. Mitigate that.
    // TODO: Also, delete this if not actually needed because fcm sync
    private suspend fun deleteEventCache(eventId: Long, setState: Boolean = false): Boolean {
        val success = eventRepository.deleteEventCache(eventId)

        if(setState) {
            mainStateIntent.setState(if(success) EventState.OnData.DeleteEventCacheResult
            else EventState.Error(Constants.cacheDeletionError))
        } else if(!success) {
            throw Exception(Constants.cacheDeletionError)
        }

        return true
    }

    init {
        handleIntent()
    }
}