package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
abstract class AuthFragmentViewModel(application: MultiDexApplication) : StateIntentViewModel<TokenState, TokenIntent>(application) {

}