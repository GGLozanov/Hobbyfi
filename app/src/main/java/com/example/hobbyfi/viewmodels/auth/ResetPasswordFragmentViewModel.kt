package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.AuthInclusiveViewModel
import com.example.hobbyfi.viewmodels.base.AuthPartialViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ResetPasswordFragmentViewModel(
    application: Application
) : AuthPartialViewModel(application) {
    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    TokenIntent.ResetPassword -> {
                        resetPassword()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun resetPassword() {
        mainStateIntent.setState(TokenState.Loading)

        mainStateIntent.setState(try {
            TokenState.ResetPasswordSuccess(tokenRepository.resetPassword(
                email.value!!
            ))
        } catch(ex: Exception) {
            ex.printStackTrace()
            TokenState.Error(
                ex.message
            )
        })
    }

    init {
        handleIntent()
    }

    override val combinedObserversInvalidity: LiveData<Boolean> get() = email.invalidity
}