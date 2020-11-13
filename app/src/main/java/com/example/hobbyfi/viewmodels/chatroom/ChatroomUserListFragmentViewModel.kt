package com.example.hobbyfi.viewmodels.chatroom

import androidx.multidex.MultiDexApplication
import androidx.paging.PagingSource
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.User
import com.example.hobbyfi.persistence.UserDao
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.viewmodels.base.PagedListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomUserListFragmentViewModel(application: MultiDexApplication) : PagedListViewModel<UserState, UserIntent, User, UserDao>(application) {
    // TODO: Upon fetching pagingdata, set the UserState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance

    override val pagingSource: PagingSource<Int, Message> by instance()

    override val _state: MutableStateFlow<UserState>
        get() = TODO("Not yet implemented")

    override fun handleIntent() {
        TODO("Not yet implemented")
    }
}