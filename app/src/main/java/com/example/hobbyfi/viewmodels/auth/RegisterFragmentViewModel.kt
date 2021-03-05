package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.data.TagBundle
import com.example.hobbyfi.shared.invalidateBy
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class RegisterFragmentViewModel(application: Application) : AuthConfirmationViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel(), Base64ImageHolder by Base64ImageHolderViewModel() {
    var tagBundle: TagBundle = TagBundle()

    init {
        handleIntent() // need to redeclare this method call in each viewModel due to handleIntent() accessing state on an unititialised object
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is TokenIntent.FetchRegisterToken -> {
                        viewModelScope.launch { // another coroutine in case of img upload not to slow down UI
                            fetchRegisterToken()
                        }
                    }
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

    override val combinedObserversInvalidity: LiveData<Boolean> get() = invalidateBy(
            name.invalidity,
            description.invalidity,
            password.invalidity,
            email.invalidity,
            confirmPassword.invalidity,
        )
}