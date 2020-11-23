package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import org.kodein.di.generic.instance

// TODO: Avoid code dup and find a way to abstract with AuthFragmentViewModel
abstract class AuthChangeDialogFragmentViewModel(application: Application)
    : BaseViewModel(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    protected val userRepository: UserRepository by instance(tag = "userRepository")
    protected val tokenRepository: TokenRepository by instance(tag = "tokenRepository")

    @Bindable
    val email: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val password: MutableLiveData<String> = MutableLiveData()
}