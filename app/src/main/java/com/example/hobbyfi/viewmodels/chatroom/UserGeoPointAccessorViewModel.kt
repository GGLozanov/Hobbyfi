package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class UserGeoPointAccessorViewModel(
    application: Application,
    protected val _relatedEvent: Event
) : StateIntentViewModel<UserGeoPointState, UserGeoPointIntent>(application) {
    protected val eventRepository: EventRepository by instance(tag = "eventRepository")

    override val mainStateIntent: StateIntent<UserGeoPointState, UserGeoPointIntent> = object : StateIntent<UserGeoPointState, UserGeoPointIntent>() {
        override val _state: MutableStateFlow<UserGeoPointState> = MutableStateFlow(
            UserGeoPointState.Idle)
    }

    val relatedEvent get() = _relatedEvent

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is UserGeoPointIntent.FetchUsersGeoPoints -> {

                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }
}