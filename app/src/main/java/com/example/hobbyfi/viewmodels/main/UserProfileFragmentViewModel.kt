package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ResponseState
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayBindable
import com.example.hobbyfi.viewmodels.base.TwoWayBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class UserProfileFragmentViewModel(application: Application)
    : StateIntentViewModel<ResponseState<Response>, UserIntent>(application), TwoWayBindable by TwoWayBindableViewModel() {
    private val userRepository: UserRepository by instance(tag = "userRepository")

    init {
        handleIntent()
    }

    override val _state: MutableStateFlow<ResponseState<Response>>
        = MutableStateFlow(ResponseState.Idle)

    override fun handleIntent() {
        viewModelScope.launch {
            intent.consumeAsFlow().collect {
                when(it) {
                    is UserIntent.UpdateUser -> {
                        updateUser(it.userUpdateFields)
                    }
                    is UserIntent.DeleteUser -> {
                        deleteUser()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    @Bindable
    val username: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val description: MutableLiveData<String?> = MutableLiveData()

    private fun updateUser(userFields: Map<String?, String?>) {
        viewModelScope.launch {
            _state.value = ResponseState.Loading

            _state.value = try {
                ResponseState.OnData(
                    userRepository.editUser(userFields)
                )
            } catch(reauthEx: Repository.ReauthenticationException) {
                ResponseState.Error(
                    Constants.reauthError,
                    shouldReauth = true
                )
            } catch(ex: Exception) {
                ResponseState.Error(
                    ex.message
                )
            }
        }
    }

    private fun deleteUser() {
        viewModelScope.launch {
            _state.value = ResponseState.Loading

            _state.value = try {
                ResponseState.OnData(
                    userRepository.deleteUser()
                )
            } catch(reauthEx: Repository.ReauthenticationException) {
                ResponseState.Error(
                    Constants.reauthError,
                    shouldReauth = true
                )
            } catch(ex: Exception) {
                ResponseState.Error(
                    ex.message
                )
            }
        }
    }
}