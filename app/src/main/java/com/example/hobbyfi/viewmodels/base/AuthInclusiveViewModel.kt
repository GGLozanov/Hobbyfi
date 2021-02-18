package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.shared.equalsOrBiggerThan
import com.example.hobbyfi.shared.equalsOrLessThan
import com.example.hobbyfi.state.TokenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthInclusiveViewModel(
    application: Application
) : AuthPartialViewModel(application) {

    @Bindable
    open val password: PredicateMutableLiveData<String> = PredicateMutableLiveData {  it == null ||
            it.isEmpty() || it.length <= 4
                || it.length >= 15 }

    protected suspend fun fetchLoginToken() {
        mainStateIntent.setState(TokenState.Loading)
        mainStateIntent.setState(try {
            TokenState.TokenReceived(tokenRepository.getLoginToken(
                email.value!!,
                password.value!!
            ))
        } catch(ex: Exception) {
            ex.printStackTrace()
            TokenState.Error(ex.message)
        })
    }
}