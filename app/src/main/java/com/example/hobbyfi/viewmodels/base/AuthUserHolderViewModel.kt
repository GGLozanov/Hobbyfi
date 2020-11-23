package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.UserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthUserHolderViewModel(application: Application)
    : StateIntentViewModel<UserState, UserIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    protected val userRepository: UserRepository by instance(tag = "userRepository")

    protected var _authUser: MutableLiveData<User?> = MutableLiveData(null) // mainactivity observes state, ProfileFragment sends intent & this fetches, lifecycle-aware stateflow receives state update & triggers UI update
    val authUser: LiveData<User?> get() = _authUser

    override val _state: MutableStateFlow<UserState> = MutableStateFlow(UserState.Idle)

//    private val _responseState: MutableStateFlow<ResponseState<Response>> = MutableStateFlow(ResponseState.Idle)
//    val responseState: StateFlow<ResponseState<Response>> get() = _responseState

    override fun handleIntent() {
        viewModelScope.launch {
            intent.consumeAsFlow().collect {
                when(it) {
                    is UserIntent.FetchUser -> {
                        fetchUser()
                    }
                    is UserIntent.UpdateUser -> {
                        updateUser(it.userUpdateFields)
                    }
                    is UserIntent.DeleteUser -> {
                        deleteUser()
                    }
                    is UserIntent.FetchUsers -> {
                        fetchUsers()
                    }
                }
            }
        }
    }

    fun setAndSaveUser(user: User) {
        _authUser.value = user
        viewModelScope.launch {
            userRepository.saveUser(user)
        }
    }

    fun updateAndSaveUser(userFields: Map<String?, String?>) {
        _authUser.value!!.updateFromFieldMap(userFields)
        viewModelScope.launch {
            userRepository.saveUser(_authUser.value!!)
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

    private fun updateUser(userFields: Map<String?, String?>) {
        viewModelScope.launch {
            _state.value = UserState.Loading

            _state.value = try {
                UserState.OnData.UserUpdateResult(
                    userRepository.editUser(userFields),
                    userFields
                )
            } catch(reauthEx: Repository.ReauthenticationException) {
                UserState.Error(
                    Constants.reauthError,
                    shouldReauth = true
                )
            } catch(ex: Exception) {
                UserState.Error(
                    ex.message
                )
            }
        }
    }

    private fun deleteUser() {
        viewModelScope.launch {
            _state.value = UserState.Loading

            _state.value = try {
                UserState.OnData.UserDeleteResult(
                    userRepository.deleteUser()
                )
            } catch(reauthEx: Repository.ReauthenticationException) {
                UserState.Error(
                    Constants.reauthError,
                    shouldReauth = true
                )
            } catch(ex: Exception) {
                UserState.Error(
                    ex.message
                )
            }
        }
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            _state.value = UserState.Loading

            // TODO: Users fetch. . .
        }
    }
}