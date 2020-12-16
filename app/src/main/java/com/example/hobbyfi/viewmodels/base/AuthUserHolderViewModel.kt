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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthUserHolderViewModel(application: Application, user: User?) : StateIntentViewModel<UserState, UserIntent>(application),
    TwoWayDataBindable by TwoWayDataBindableViewModel() {
    protected val userRepository: UserRepository by instance(tag = "userRepository")

    protected var _authUser: MutableLiveData<User?> = MutableLiveData(user)
    val authUser: LiveData<User?> get() = _authUser

    override val _mainState: MutableStateFlow<UserState> = MutableStateFlow(UserState.Idle)

    protected var _latestTagUpdateFail: MutableLiveData<Boolean> = MutableLiveData(false)
    val latestTagUpdateFail: LiveData<Boolean> get() = _latestTagUpdateFail

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
    open fun setUser(user: User) {
        _authUser.value = user
    }

    fun updateAndSaveUser(userFields: Map<String?, String?>) {
        _authUser.value = _authUser.value!!.updateFromFieldMap(userFields)
        viewModelScope.launch {
            userRepository.saveUser(_authUser.value!!)
        }
    }

    private suspend fun deleteUserCache() {
        userRepository.deleteUser(_authUser.value!!)
        _authUser.value = null
    }

    private suspend fun fetchUser() {
        _mainState.value = UserState.Loading
        // observe flow returned from networkBoundFetcher and change state upon emitted value
        userRepository.getUser().catch { e ->
            e.printStackTrace()
            _mainState.value = when(e) {
                is Repository.ReauthenticationException, is InstantiationException, is InstantiationError, is Repository.NetworkException, is CancellationException -> {
                    UserState.Error(
                        e.message,
                        shouldReauth = true
                    )
                }
                else -> UserState.Error(
                    e.message
                )
            }
        }.collect {
            if(it != null) {
                setUser(it)
                _mainState.value = UserState.OnData.UserResult(it)
            }
        }
    }

    private suspend fun updateUser(userFields: Map<String?, String?>) {
        val userIsUpdatingTags = userFields.containsKey(Constants.TAGS + "[]")
        _mainState.value = UserState.Loading

        _mainState.value = try {
            val result = UserState.OnData.UserUpdateResult(
                userRepository.editUser(userFields),
                userFields
            )

            if(userIsUpdatingTags) { // hacky fix for user selecting tags and tags not being updated => resetting tags in UserProfileFragment UI
                _latestTagUpdateFail.value = false
            }

            result
        } catch(ex: Exception) {
            if(userIsUpdatingTags) {
                _latestTagUpdateFail.value = true
            }

            ex.printStackTrace()
            when(ex) {
                is Repository.ReauthenticationException, is InstantiationException, is InstantiationError, is Repository.NetworkException -> {
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
            val response = UserState.OnData.UserDeleteResult(
                userRepository.deleteUser()
            )

            deleteUserCache()

            response
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