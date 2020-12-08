package com.example.hobbyfi.viewmodels.base

import android.app.Application
import android.util.Log
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
abstract class AuthUserHolderViewModel(application: Application, user: User?)
    : StateIntentViewModel<UserState, UserIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    init {
        // TODO: Check if this gets called every time in bottomnav
        if(user != null) {
            Log.i("AuthUserHolderVM", "Saving auth user instance")
            viewModelScope.launch {
                userRepository.saveUser(user)
            }
        }
    }

    protected val userRepository: UserRepository by instance(tag = "userRepository")

    protected var _authUser: MutableLiveData<User?> = MutableLiveData(user)
    val authUser: LiveData<User?> get() = _authUser

    override val _mainState: MutableStateFlow<UserState> = MutableStateFlow(UserState.Idle)

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collectLatest { // collect latest doesn't cancel the [action] block after receiving a value
                when(it) {
                    is UserIntent.FetchUser -> {
                        fetchUser()
                    }
                    is UserIntent.UpdateUser -> {
                        Log.i("AuthUserHolderVM", "Update User intent sent")
                        updateUser(it.userUpdateFields)
                    }
                    is UserIntent.DeleteUser -> {
                        Log.i("AuthUserHolderVM", "Delete User intent sent")
                        deleteUser()
                    }
                }
            }
        }
    }

    // user fetched - already saved from networkboundfetcher
    fun setUser(user: User) {
        _authUser.value = user
    }

    fun updateAndSaveUser(userFields: Map<String?, String?>) {
        _authUser.value = _authUser.value!!.updateFromFieldMap(userFields)
        viewModelScope.launch {
            userRepository.saveUser(_authUser.value!!)
        }
    }

    private suspend fun fetchUser() {
        _mainState.value = UserState.Loading
        // observe flow returned from networkBoundFetcher and change state upon emitted value
        userRepository.getUser().catch { e ->
            e.printStackTrace()
            _mainState.value = if(e is Repository.ReauthenticationException)
                UserState.Error(Constants.reauthError, shouldReauth = true) else UserState.Error(e.message)
        }.collect {
            if(it != null) {
                _mainState.value = UserState.OnData.UserResult(it)
            }
        }
    }

    private suspend fun updateUser(userFields: Map<String?, String?>) {
        _mainState.value = UserState.Loading

        _mainState.value = try {
            UserState.OnData.UserUpdateResult(
                userRepository.editUser(userFields),
                userFields
            )
        } catch(ex: Exception) {
            ex.printStackTrace()
            when(ex) {
                // FIXME: how tf do you chain `is` checks?
                is Repository.ReauthenticationException, is InstantiationException, is InstantiationError -> {
                    UserState.Error(
                        ex.message,
                        shouldReauth = true
                    )
                }
                else -> UserState.Error(
                    ex.message
                )
            }
        }
    }

    private suspend fun deleteUser() {
        _mainState.value = UserState.Loading

        _mainState.value = try {
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