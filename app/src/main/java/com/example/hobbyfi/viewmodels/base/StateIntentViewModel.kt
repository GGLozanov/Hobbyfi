package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalCoroutinesApi
abstract class StateIntentViewModel<T: State, E: Intent>(application: Application) : BaseViewModel(application) {

    protected abstract val _state: MutableStateFlow<T>

    val state: StateFlow<T>
        get() = _state

    protected val intent: Channel<E> = Channel(Channel.UNLIMITED)

    protected abstract fun handleIntent()

    suspend fun sendIntent(i: E) {
        intent.send(i)
    }
}