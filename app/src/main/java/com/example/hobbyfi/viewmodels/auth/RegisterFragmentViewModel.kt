package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
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

    override val _mainState: MutableStateFlow<TokenState> = MutableStateFlow(TokenState.Idle)

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collect {
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

    private var _base64Image: String? = null
    val base64Image get() = _base64Image

    fun setProfileImageBase64(base64Image: String) {
        _base64Image = base64Image
    }

    private fun fetchRegisterToken() {
        Log.i("RegisterFragmentVM", "fetchRegisterToken called")
        viewModelScope.launch {
            _mainState.value = TokenState.Loading
            _mainState.value = try {
                TokenState.TokenReceived(tokenRepository.getRegisterToken(
                    null,
                    email.value,
                    password.value,
                    username.value!!,
                    description.value,
                    base64Image,
                    selectedTags
                ))
            } catch (e: Exception) {
                e.printStackTrace()
                TokenState.Error(e.message) // TODO: More specific error handling w/ custom exceptions
            }
        }
    }

    fun appendNewSelectedTagsToTags(selectedTags: List<Tag>) {
        selectedTags.forEach {
            if(!_tags.contains(it)) {
                _tags.add(it)
            }
        }
    }
}