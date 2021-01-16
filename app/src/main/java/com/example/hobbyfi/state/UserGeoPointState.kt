package com.example.hobbyfi.state

import androidx.lifecycle.LiveData
import com.example.hobbyfi.models.UserGeoPoint
import kotlinx.coroutines.flow.StateFlow

sealed class UserGeoPointState : State {
    object Idle : UserGeoPointState()
    object Loading : UserGeoPointState()

    sealed class OnData : UserGeoPointState() {
        data class OnUserGeoPointResult(val userGeoPoint: StateFlow<UserGeoPoint>) : OnData()
        data class OnUsersGeoPointsResult(val userGeoPoints: StateFlow<List<UserGeoPoint>>) : OnData()
        data class OnUserGeoPointSetResult(val setUserGeoPoint: LiveData<UserGeoPoint>) : OnData()
    }

    data class Error(val error: String?, val shouldReauth: Boolean = false) : UserGeoPointState()
}