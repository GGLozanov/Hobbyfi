package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindable
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventEditFragmentViewModel(application: Application, private val event: Event) : EventAccessorViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
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
            EventState.OnData.EventEditResult(eventRepository.editEvent(
                updateFields
            ), updateFields)
        } catch(ex: Exception) {
            ex.printStackTrace()

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