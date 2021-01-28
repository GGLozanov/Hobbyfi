package com.example.hobbyfi.viewmodels.base

import android.app.Application
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.state.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalCoroutinesApi
abstract class StateIntentViewModel<T: State, E: Intent>(
    application: Application
) : BaseViewModel(application) {
    // wouldn't have been an abstract val if there had been a way to have inheritance between sealed classes :(
    // and I could just reference the base state `Idle` for the MutableStateFlow initialisation
    protected abstract val mainStateIntent: StateIntent<T, E>

    protected abstract fun handleIntent() // has more relating to ViewModel, which is why it's outside StateIntent

    suspend fun sendIntent(i: E) {
        mainStateIntent.sendIntent(i)
    }

    val mainState: StateFlow<T> get() = mainStateIntent.state
}