package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.viewmodels.base.AuthConfirmationViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
abstract class AuthChangeDialogFragmentViewModel(application: Application) : AuthConfirmationViewModel(application) {
    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is TokenIntent.FetchLoginToken -> {
                        fetchLoginToken()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }
}