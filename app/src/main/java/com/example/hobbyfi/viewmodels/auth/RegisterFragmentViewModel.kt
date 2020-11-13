package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class RegisterFragmentViewModel(application: MultiDexApplication) : AuthFragmentViewModel(application), Observable {
    override val _state: MutableStateFlow<TokenState> = MutableStateFlow(TokenState.Idle)

    override fun handleIntent() {
        viewModelScope.launch {
            intent.consumeAsFlow().collect {
                when(it) {
                    is TokenIntent.FetchRegisterToken -> fetchRegisterToken()
                }
            }
        }
    }

    // TODO: Check encapsulation principles for exposing MutableLiveData for two-way databinding
    @Bindable
    val email: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val password: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val username: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val description: MutableLiveData<String> = MutableLiveData()

    val tags: MutableLiveData<List<Tag>> = MutableLiveData(Constants.predefinedTags)
    // this livedata instance will be initialised once the tag selection dialog fragment finishes its workTag
    // and sends its livedata instance through the fragment-dialog listener, which will be set to this instance

    private fun fetchRegisterToken() {
        viewModelScope.launch {
            _state.value = TokenState.Loading
            _state.value = try {
                TokenState.OnTokenReceived(tokenRepository.getRegisterToken(
                    email.value!!,
                    password.value!!,
                    username.value!!,
                    description.value!!,
                    tags?.value!!
                ))
            } catch (e: Exception) {
                TokenState.Error(e.localizedMessage) // TODO: More specific error handling w/ custom exceptions
            }
        }
    }
}