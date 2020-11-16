package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.multidex.MultiDexApplication
import androidx.paging.PagingSource
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.persistence.ChatroomDao
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomListFragmentViewModel(application: Application) : PagedListViewModel<ChatroomState, ChatroomIntent, Chatroom, ChatroomDao>(application) {
    // TODO: Upon fetching pagingdata, set the ChatroomState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance
    init {
        handleIntent()
    }

    override val pagingSource: PagingSource<Int, Message> by instance()

    override val _state: MutableStateFlow<ChatroomState> = MutableStateFlow(ChatroomState.Idle)

    override fun handleIntent() {
        TODO("Not yet implemented")
    }
}