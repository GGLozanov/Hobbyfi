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
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class UserGeoPointAccessorViewModel(
    application: Application,
    protected var _relatedEvent: Event
) : StateIntentViewModel<UserGeoPointState, UserGeoPointIntent>(application) {
    protected val eventRepository: EventRepository by instance(tag = "eventRepository")

    override val mainStateIntent: StateIntent<UserGeoPointState, UserGeoPointIntent> = object : StateIntent<UserGeoPointState, UserGeoPointIntent>() {
        override val _state: MutableStateFlow<UserGeoPointState> = MutableStateFlow(UserGeoPointState.Idle)
    }

    val relatedEvent get() = _relatedEvent

    protected var _userGeoPoints: MutableLiveData<List<UserGeoPoint>>? = null
    val userGeoPoints: LiveData<List<UserGeoPoint>>? = _userGeoPoints

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is UserGeoPointIntent.FetchUsersGeoPoints -> {
                        getUserGeoPoints(it.authGeoPointUsername)
                    }
                    is UserGeoPointIntent.UpdateUserGeoPoint -> {
                        // UserGeoPoint props handed as separate arguments because of UserGeoPoint immutability
                        updateUserGeoPoint(it.username, it.chatroomId, it.eventIds, it.geoPoint)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    protected fun updateUserGeoPoint(
        username: String,
        chatroomIds: List<Long>,
        eventIds: List<Long>,
        geoPoint: GeoPoint
    ) {
        mainStateIntent.setState(UserGeoPointState.Loading)

        mainStateIntent.setState(try {
            UserGeoPointState.OnData.OnUserGeoPointSetResult(
                eventRepository.setEventUserGeoPoints(
                    username,
                    chatroomIds,
                    eventIds,
                    geoPoint
                )
            )
        } catch(ex: java.lang.Exception) {
            UserGeoPointState.Error(ex.message)
        })
    }

    protected fun getUserGeoPoints(geoPointUserUsername: String?) {
        mainStateIntent.setState(UserGeoPointState.Loading)

        mainStateIntent.setState(try {
            _userGeoPoints = eventRepository.getEventUsersGeoPoint(_relatedEvent.id, geoPointUserUsername)

            UserGeoPointState.OnData.OnUsersGeoPointsResult(
                _userGeoPoints!!
            )
        } catch(ex: Exception) {
            UserGeoPointState.Error(ex.message)
        })
    }


    fun setUserGeoPoints(geoPoints: List<UserGeoPoint>) {
        _userGeoPoints?.value = geoPoints
    }

    fun forceUserGeoPointsObservation() {
        _userGeoPoints?.value = _userGeoPoints?.value
    }
}