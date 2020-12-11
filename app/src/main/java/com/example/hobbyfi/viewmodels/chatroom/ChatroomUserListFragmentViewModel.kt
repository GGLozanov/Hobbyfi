package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.state.UserListState
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class ChatroomUserListFragmentViewModel(application: Application) : StateIntentViewModel<UserListState, UserListIntent>(application) {
    // TODO: Upon fetching pagingdata, set the UserState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance
    init {
        handleIntent()
    }

    override val _mainState: MutableStateFlow<UserListState> = MutableStateFlow(UserListState.Idle)

    override fun handleIntent() {
    }
}