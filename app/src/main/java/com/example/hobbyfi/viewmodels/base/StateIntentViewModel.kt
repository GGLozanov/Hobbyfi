package com.example.hobbyfi.viewmodels.base

import android.app.Application
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.state.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalCoroutinesApi
abstract class StateIntentViewModel<T: State, E: Intent>(application: Application) : BaseViewModel(application) {

    protected abstract val _mainState: MutableStateFlow<T>

    val mainState: StateFlow<T>
        get() = _mainState

    protected val mainIntent: Channel<E> = Channel(Channel.UNLIMITED)

    protected abstract fun handleIntent()

    suspend fun sendIntent(i: E) {
        mainIntent.send(i)
    }
}