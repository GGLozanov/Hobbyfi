package com.example.hobbyfi.viewmodels.base

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.UserState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthUserHolderViewModel(application: Application, user: User?) : StateIntentViewModel<UserState, UserIntent>(application),
        TwoWayDataBindable by TwoWayDataBindableViewModel() {
    protected val userRepository: UserRepository by instance(tag = "userRepository")

    protected var _authUser: MutableLiveData<User?> = MutableLiveData(user)
    val authUser: LiveData<User?> get() = _authUser

    override val mainStateIntent: StateIntent<UserState, UserIntent> = object : StateIntent<UserState, UserIntent>() {
        override val _state: MutableStateFlow<UserState> = MutableStateFlow(UserState.Idle)
    }

    protected var _latestTagUpdateFail: MutableLiveData<Boolean> = MutableLiveData(false)
    val latestTagUpdateFail: LiveData<Boolean> get() = _latestTagUpdateFail

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest { // collect latest doesn't cancel the [action] block after receiving a value
                when(it) {
                    is UserIntent.FetchUser -> {
                        fetchUser()
                    }
                    is UserIntent.UpdateUser -> {
                        Log.i("AuthUserHolderVM", "Update User intent sent")
                        updateUser(it.userUpdateFields)
                    }
                    is UserIntent.UpdateUserCache -> {
                        updateAndSaveUser(it.userUpdateFields)
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

    protected fun updateAndSaveUser(userFields: Map<String?, String?>) {
        viewModelScope.launch {
            val updatedUser = _authUser.value!!.updateFromFieldMap(userFields)
            saveUser(updatedUser)
            _authUser.value = updatedUser
        }
    }

    protected suspend fun saveUser(updatedUser: User, shouldWritePrefTime: Boolean = true)
        = userRepository.saveUser(updatedUser, shouldWritePrefTime)

    private suspend fun deleteUserCache(shouldWritePrefTime: Boolean = true) {
        userRepository.deleteUserCache(_authUser.value!!.id, shouldWritePrefTime)
        _authUser.value = null
    }

    private suspend fun fetchUser() {
        mainStateIntent.setState(UserState.Loading)
        // observe flow returned from networkBoundFetcher and change state upon emitted value
        userRepository.getUser().catch { e ->
            e.printStackTrace()
            mainStateIntent.setState(
                UserState.Error(
                    e.message,
                    shouldReauth = (e as Exception).isCritical
                ))
        }.collect {
            if(it != null) {
                setUser(it)
                mainStateIntent.setState(UserState.OnData.UserResult(it))
            }
        }
    }

    private suspend fun updateUser(userFields: Map<String?, String?>) {
        val userIsUpdatingTags = userFields.containsKey(Constants.TAGS + "[]")
        mainStateIntent.setState(UserState.Loading)

        mainStateIntent.setState(try {
            val result = UserState.OnData.UserUpdateResult(
                userRepository.editUser(userFields),
                userFields
            )

            updateAndNotifyTagUpdateFail(userIsUpdatingTags, false)
            
            result
        } catch(ex: Exception) {
            updateAndNotifyTagUpdateFail(userIsUpdatingTags, true)

            ex.printStackTrace()
            UserState.Error(
                ex.message,
                shouldReauth = ex.isCritical
            )
        })
    }

    private suspend fun deleteUser() {
        mainStateIntent.setState(UserState.Loading)

        mainStateIntent.setState(try {
            val response = UserState.OnData.UserDeleteResult(
                userRepository.deleteUser()
            )

            deleteUserCache()

            response
        } catch(ex: Exception) {
            UserState.Error(
                ex.message,
                shouldReauth = ex.isCritical
            )
        })
    }

    private fun updateAndNotifyTagUpdateFail(userUpdatingTags: Boolean, failed: Boolean) {
        if(userUpdatingTags) { // hacky fix for user selecting tags and tags not being updated => resetting tags in UserProfileFragment UI
            _latestTagUpdateFail.apply {
                value = false
            }
        }
    }
}