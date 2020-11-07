package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.multidex.MultiDexApplication
import androidx.paging.PagedList
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.persistence.ChatroomDao
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.PagedListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class ChatroomListFragmentViewModel(application: MultiDexApplication) : PagedListViewModel<ChatroomState, ChatroomIntent, Chatroom, ChatroomDao>(application) {
    // TODO: Upon fetching pagingdata, set the ChatroomState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance

    override val _state: MutableStateFlow<ChatroomState>
        get() = TODO("Not yet implemented")

    override fun handleIntent() {
        TODO("Not yet implemented")
    }
}