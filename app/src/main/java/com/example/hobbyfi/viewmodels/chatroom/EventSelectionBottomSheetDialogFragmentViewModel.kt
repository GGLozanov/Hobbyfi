package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventSelectionBottomSheetDialogFragmentViewModel(application: Application) : EventAccessorViewModel(application) {

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventIntent.DeleteEvent -> {
                        deleteEvent(it.eventId)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun deleteEvent(eventId: Long) {
        mainStateIntent.setState(EventState.Loading)

        mainStateIntent.setState(try {
            EventState.OnData.EventDeleteResult(
                eventRepository.deleteEvent(eventId), eventId
            )
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