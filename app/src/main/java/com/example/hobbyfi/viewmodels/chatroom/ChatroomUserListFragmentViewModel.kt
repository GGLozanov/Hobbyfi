package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.persistence.UserDao
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.PagedListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class ChatroomUserListFragmentViewModel(application: MultiDexApplication) : PagedListViewModel<UserState, UserIntent, User, UserDao>(application) {
    // TODO: Upon fetching pagingdata, set the UserState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance

    override val _state: MutableStateFlow<UserState>
        get() = TODO("Not yet implemented")

    override fun handleIntent() {
        TODO("Not yet implemented")
    }
}