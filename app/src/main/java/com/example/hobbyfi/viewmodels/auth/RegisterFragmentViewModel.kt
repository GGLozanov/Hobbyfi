package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Base64Image
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.Base64ImageHolder
import com.example.hobbyfi.viewmodels.base.Base64ImageHolderViewModel
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindable
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class RegisterFragmentViewModel(application: Application) : AuthFragmentViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel(), Base64ImageHolder by Base64ImageHolderViewModel() {

    init {
        handleIntent() // need to redeclare this method call in each viewModel due to handleIntent() accessing state on an unititialised object
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is TokenIntent.FetchRegisterToken -> fetchRegisterToken()
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun fetchRegisterToken() {
        Log.i("RegisterFragmentVM", "fetchRegisterToken called")
        mainStateIntent.setState(TokenState.Loading)
        mainStateIntent.setState(try {
            TokenState.TokenReceived(tokenRepository.getRegisterToken(
                null,
                email.value,
                password.value,
                name.value!!,
                description.value,
                base64Image.base64,
                tagBundle.selectedTags
            ))
        } catch (e: Exception) {
            e.printStackTrace()
            TokenState.Error(e.message) // TODO: More specific error handling w/ custom exceptions
        })
    }
}