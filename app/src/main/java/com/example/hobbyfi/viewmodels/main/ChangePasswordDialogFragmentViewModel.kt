package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.shared.equalsOrBiggerThan
import com.example.hobbyfi.shared.equalsOrLessThan
import com.example.hobbyfi.shared.validateBy
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChangePasswordDialogFragmentViewModel(application: Application) : AuthChangeDialogFragmentViewModel(application) {
    init {
        handleIntent()
    }

    @Bindable
    val newPassword: PredicateMutableLiveData<String> = PredicateMutableLiveData { it == null ||
        it.isEmpty() || it.length <= 4 || it.length >= 15
    }

    override val combinedObserversInvalidity: LiveData<Boolean>
        get() = validateBy(
            password.invalidity,
            newPassword.invalidity,
            email.invalidity
        )
}