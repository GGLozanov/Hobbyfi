package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ResponseState
import com.example.hobbyfi.state.UserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthUserHolderViewModel(application: Application)
    : StateIntentViewModel<UserState, UserIntent>(application), TwoWayBindable by TwoWayBindableViewModel() {
    protected val userRepository: UserRepository by instance(tag = "userRepository")

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
            // observe flow returned from networkBoundFetcher and change state upon emitted value
            userRepository.getUser().catch { e ->
                _state.value = if(e is Repository.ReauthenticationException)
                    UserState.Error(Constants.reauthError, shouldReauth = true) else UserState.Error(e.message)
            }.collect {
                if(it != null) {
                    _state.value = UserState.OnData.UserResult(it)
                }
            }
        }
    }
}