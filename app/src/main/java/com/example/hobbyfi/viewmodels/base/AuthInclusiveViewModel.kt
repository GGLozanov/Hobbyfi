package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.state.TokenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthInclusiveViewModel(application: Application) : StateIntentViewModel<TokenState, TokenIntent>(application),
    TwoWayDataBindable by TwoWayDataBindableViewModel() {
    // will allow subclasses to override handleIntent() to
    // handle invalid intents and impose some form of order in an otherwise tightly coupled inheritance hierarchy

    protected val tokenRepository: TokenRepository by instance(tag = "tokenRepository")

    override val _mainState: MutableStateFlow<TokenState>
            = MutableStateFlow(TokenState.Idle)

    @Bindable
    val email: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val password: MutableLiveData<String> = MutableLiveData()

    protected suspend fun fetchLoginToken() {
        _mainState.value = TokenState.Loading
        _mainState.value = try {
            TokenState.TokenReceived(tokenRepository.getLoginToken(
                email.value!!,
                password.value!!
            ))
        } catch(ex: Exception) {
            ex.printStackTrace()
            TokenState.Error(ex.localizedMessage)
        }
    }
}