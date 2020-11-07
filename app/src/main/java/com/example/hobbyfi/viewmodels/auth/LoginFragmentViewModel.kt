package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class LoginFragmentViewModel(application: MultiDexApplication) : AuthFragmentViewModel(application) {
    override val _state: MutableStateFlow<TokenState>
        get() = TODO("Not yet implemented")

    override fun handleIntent() {
        TODO("Not yet implemented")
    }
    // TODO: Implement the ViewModel
}