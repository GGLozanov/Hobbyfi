package com.example.hobbyfi.viewmodels.main

import android.app.Application
import android.util.Log
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.shared.invalidateBy
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChangePasswordDialogFragmentViewModel(application: Application) : AuthChangeDialogFragmentViewModel(application) {
    init {
        handleIntent()
    }

    @Bindable
    val newPassword: PredicateMutableLiveData<String> = PredicateMutableLiveData {
        it == null || it.isEmpty() || it.length <= 4
                || it.length >= 15
    }

    @Bindable
    override val password: PredicateMutableLiveData<String> = PredicateMutableLiveData { it == null ||
            it.isEmpty() || it.length <= 4 || it.length >= 15
    }

    @Bindable
    override val confirmPassword: PredicateMutableLiveData<String> = PredicateMutableLiveData {
        it == null || it.isEmpty() || it != newPassword.value
    }

    override val combinedObserversInvalidity: LiveData<Boolean>
        get() = invalidateBy(
                password.invalidity,
                newPassword.invalidity,
                confirmPassword.invalidity
            )
}