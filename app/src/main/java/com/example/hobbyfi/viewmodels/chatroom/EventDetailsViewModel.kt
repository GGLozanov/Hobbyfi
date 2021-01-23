package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventDetailsViewModel(
    application: Application,
    event: Event
) : UserGeoPointAccessorViewModel(application, event) {
    // TODO: Implement the ViewModel

    // TODO: Fetch other users & user geopoint here (?)
    private val _userGeoPoints: MutableLiveData<List<UserGeoPoint>> = MutableLiveData()
    val userGeoPoints: LiveData<List<UserGeoPoint>> = _userGeoPoints

    init {
        handleIntent()
    }
}