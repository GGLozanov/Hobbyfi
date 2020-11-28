package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

// TODO: Avoid code dup and find a way to abstract with AuthFragmentViewModel
@ExperimentalCoroutinesApi
class AuthChangeDialogFragmentViewModel(application: Application)
    : AuthInclusiveViewModel(application) {

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collect {
                when(it) {
                    is TokenIntent.FetchLoginToken -> {
                        fetchLoginToken()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    @Bindable
    val confirmPassword: MutableLiveData<String> = MutableLiveData()
}