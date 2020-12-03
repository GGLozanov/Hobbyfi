package com.example.hobbyfi.viewmodels.main

import android.app.Application
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class ChatroomListFragmentViewModel(application: Application) : StateIntentViewModel<ChatroomListState, ChatroomListIntent>(application) {
    // TODO: Upon fetching pagingdata, set the ChatroomState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance
    init {
        handleIntent()
    }

    override val _mainState: MutableStateFlow<ChatroomListState> = MutableStateFlow(ChatroomListState.Idle)

    override fun handleIntent() {
    }
}