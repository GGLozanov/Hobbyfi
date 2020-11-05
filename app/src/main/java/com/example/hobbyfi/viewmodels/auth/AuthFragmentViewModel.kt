package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.provider.Contacts.PresenceColumns.IDLE
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

abstract class AuthFragmentViewModel(application: Application) : BaseViewModel(application) {
    @ExperimentalCoroutinesApi
    private val tokenState: MutableStateFlow<TokenState> = MutableStateFlow(TokenState.Idle)
}