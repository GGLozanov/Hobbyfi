package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindable
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@ExperimentalCoroutinesApi
@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class RegisterFragmentViewModel(application: Application) : AuthFragmentViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {

    init {
        handleIntent() // need to redeclare this method call in each viewModel due to handleIntent() accessing state on an unititialised object
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collectLatest {
                when(it) {
                    is TokenIntent.FetchRegisterToken -> fetchRegisterToken()
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private var _base64Image: String? = null
    val base64Image get() = _base64Image

    fun setProfileImageBase64(base64Image: String) {
        _base64Image = base64Image
    }

    private suspend fun fetchRegisterToken() {
        Log.i("RegisterFragmentVM", "fetchRegisterToken called")
        _mainState.value = TokenState.Loading
        _mainState.value = try {
            TokenState.TokenReceived(tokenRepository.getRegisterToken(
                null,
                email.value,
                password.value,
                name.value!!,
                description.value,
                base64Image,
                tagBundle.selectedTags
            ))
        } catch (e: Exception) {
            e.printStackTrace()
            TokenState.Error(e.message) // TODO: More specific error handling w/ custom exceptions
        }
    }
}