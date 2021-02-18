package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.hobbyfi.shared.validateBy
import com.example.hobbyfi.viewmodels.base.AuthConfirmationViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChangeEmailDialogFragmentViewModel(application: Application) : AuthChangeDialogFragmentViewModel(application) {
    init {
        handleIntent()
    }

    private var _newEmail: String? = null
    val newEmail get() = _newEmail

    fun setNewEmail(email: String?) {
        _newEmail = email
    }

    override val combinedObserversInvalidity: LiveData<Boolean>
        get() = validateBy(
            email.invalidity,
            password.invalidity,
            confirmPassword.invalidity
        )
}