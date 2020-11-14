package com.example.hobbyfi.viewmodels.base

import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.state.UserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
abstract class AuthUserHolderViewModel(application: MultiDexApplication) : StateIntentViewModel<UserState, UserIntent>(application) {
    protected var _authUser: User? = null
    // mainactivity observes state, ProfileFragment sends intent & this fetches, lifecycle-aware stateflow receives state update & triggers UI update

    override val _state: MutableStateFlow<UserState> = MutableStateFlow(UserState.Idle)

    override fun handleIntent() {
        TODO("Not yet implemented")
    }

    fun setUser(user: User) {
        _authUser = user
        _state.value = UserState.OnData.UserResult(user)
    }

    fun fetchUser() {
        _state.value = UserState.Loading

    }
}