package com.example.hobbyfi.viewmodels.base

import android.app.Application
import android.util.Patterns
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.state.TokenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthPartialViewModel(
    application: Application
) : StateIntentViewModel<TokenState, TokenIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    // will allow subclasses to override handleIntent() to
    // handle invalid intents and impose some form of order in an otherwise tightly coupled inheritance hierarchy

    protected val tokenRepository: TokenRepository by instance(tag = "tokenRepository")

    override val mainStateIntent: StateIntent<TokenState, TokenIntent> = object : StateIntent<TokenState, TokenIntent>() {
        override val _state: MutableStateFlow<TokenState> = MutableStateFlow(TokenState.Idle)
    }

    fun resetTokenState() = mainStateIntent.setState(TokenState.Idle)

    @Bindable
    val email: PredicateMutableLiveData<String> = PredicateMutableLiveData { it == null ||
        it.isEmpty()  || !Patterns.EMAIL_ADDRESS.matcher(it).matches()
    }
}