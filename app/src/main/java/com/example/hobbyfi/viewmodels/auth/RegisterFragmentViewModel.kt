package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.state.TokenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class RegisterFragmentViewModel(application: Application) : AuthFragmentViewModel(application) {
    init {
        handleIntent() // need to redeclare this method call in each viewModel due to handleIntent() accessing state on an unititialised object
    }

    override val _state: MutableStateFlow<TokenState> = MutableStateFlow(TokenState.Idle)

    override fun handleIntent() {
        viewModelScope.launch {
            intent.consumeAsFlow().collect {
                when(it) {
                    is TokenIntent.FetchRegisterToken -> fetchRegisterToken()
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    @Bindable
    val username: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val description: MutableLiveData<String> = MutableLiveData()

    private var base64Image: String? = null

    fun setProfileImageBase64(base64Image: String) {
        this.base64Image = base64Image
    }

    fun getProfileImageBase64() : String? {
        return base64Image
    }

    private fun fetchRegisterToken() {
        viewModelScope.launch {
            _state.value = TokenState.Loading
            _state.value = try {
                TokenState.OnTokenReceived(tokenRepository.getRegisterToken(
                    null,
                    email.value!!,
                    password.value!!,
                    username.value!!,
                    description.value!!,
                    base64Image,
                    _selectedTags.value
                ))
            } catch (e: Exception) {
                TokenState.Error(e.localizedMessage) // TODO: More specific error handling w/ custom exceptions
            }
        }
    }

}