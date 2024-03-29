package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.databinding.Bindable
import com.example.hobbyfi.shared.PredicateMutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class AuthConfirmationViewModel(
    application: Application
) : AuthInclusiveViewModel(application) {
    @Bindable
    open val confirmPassword: PredicateMutableLiveData<String> = PredicateMutableLiveData {
        it == null || it.isEmpty() || it != password.value
    }
}