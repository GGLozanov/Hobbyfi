package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import androidx.paging.PagingSource
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.persistence.MessageDao
import com.example.hobbyfi.state.MessageState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.PagedListViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayBindable
import com.example.hobbyfi.viewmodels.base.TwoWayBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomMessageListFragmentViewModel(application: Application) :
    PagedListViewModel<MessageState, MessageIntent, Message, MessageDao>(application) {
    // TODO: Upon fetching pagingdata, set the messageState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance
    override val pagingSource: PagingSource<Int, Message> by instance()

    @Bindable
    val message: MutableLiveData<String> = MutableLiveData()

    override val _state: MutableStateFlow<MessageState> = MutableStateFlow(MessageState.Idle)

    override fun handleIntent() {
        TODO("Not yet implemented")
    }

}