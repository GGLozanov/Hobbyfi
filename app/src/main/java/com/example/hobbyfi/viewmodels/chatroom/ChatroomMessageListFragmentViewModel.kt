package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.persistence.MessageDao
import com.example.hobbyfi.state.MessageState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.PagedListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class ChatroomMessageListFragmentViewModel(application: MultiDexApplication) : PagedListViewModel<MessageState, MessageIntent, Message, MessageDao>(application) {
    // TODO: Upon fetching pagingdata, set the messageState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance

    override val _state: MutableStateFlow<MessageState>
        get() = TODO("Not yet implemented")

    override fun handleIntent() {
        TODO("Not yet implemented")
    }
}