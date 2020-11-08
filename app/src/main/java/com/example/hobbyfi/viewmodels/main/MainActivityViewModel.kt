package com.example.hobbyfi.viewmodels.main

import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class MainActivityViewModel(application: MultiDexApplication) : StateIntentViewModel<UserState, UserIntent>(application) {
    private val _authUser: User? = null
    // mainactivity observes state, ProfileFragment sends intent & this fetches, lifecycle-aware stateflow receives state update & triggers UI update

    override val _state: MutableStateFlow<UserState>
        get() = MutableStateFlow(UserState.Idle)

    override fun handleIntent() {
        TODO("Not yet implemented")
    }
}