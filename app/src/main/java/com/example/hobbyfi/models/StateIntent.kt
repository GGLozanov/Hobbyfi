package com.example.hobbyfi.models

import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.state.State
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow

abstract class StateIntent<T: State, E: Intent> {
    protected abstract val _state: MutableStateFlow<T>

    val state: StateFlow<T>
        get() = _state

    protected val intent: Channel<E> = Channel(Channel.UNLIMITED)

    suspend fun sendIntent(i: E) {
        intent.send(i)
    }

    fun intentAsFlow(): Flow<E> = intent.consumeAsFlow()

    fun setState(state: T) {
        _state.value = state
    }
}