package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.state.UserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
abstract class AuthUserHolderViewModel(application: Application)
    : StateIntentViewModel<UserState, UserIntent>(application), TwoWayBindable by TwoWayBindableViewModel() {
    protected var _authUser: User? = null
    // mainactivity observes state, ProfileFragment sends intent & this fetches, lifecycle-aware stateflow receives state update & triggers UI update

    override val _state: MutableStateFlow<UserState> = MutableStateFlow(UserState.Idle)

    override fun handleIntent() {
        TODO("Not yet implemented")
    }

    fun setUser(user: User?) {
        _authUser = user
        if(user != null) {
            _state.value = UserState.OnData.UserResult(user)
        } else {
            fetchUser()
        }
    }

    private fun fetchUser() {
        viewModelScope.launch {
            _state.value = UserState.Loading

        }
    }
}