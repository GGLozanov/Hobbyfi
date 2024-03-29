package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.Base64ImageHolder
import com.example.hobbyfi.viewmodels.base.Base64ImageHolderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventEditDialogFragmentViewModel(
    application: Application,
    private val _event: Event
) : EventAccessorViewModel(application), Base64ImageHolder by Base64ImageHolderViewModel() {

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventIntent.UpdateEvent -> {
                        viewModelScope.launch { // img upload safeguard
                            updateEvent(it.eventUpdateFields)
                        }
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    val event get() = _event

    private suspend fun updateEvent(updateFields: Map<String, String?>) {
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

        name.value = event.name
        description.value = event.description
    }
}