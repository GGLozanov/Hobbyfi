package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.viewmodels.base.AuthUserHolderViewModel
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class MainActivityViewModel(application: Application, user: User?)
    : AuthUserHolderViewModel(application, user) {

    init {
        handleIntent()
    }
}