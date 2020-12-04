package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class ChatroomMessageListFragmentViewModel(application: Application) :
    StateIntentViewModel<MessageListState, MessageListIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    // TODO: Upon fetching pagingdata, set the messageState to onData and pass in the pagingdata in order to trigger observer in view
    // TODO: Kodein data source instance

    @Bindable
    val message: MutableLiveData<String> = MutableLiveData()

    override val _mainState: MutableStateFlow<MessageListState> = MutableStateFlow(MessageListState.Idle)

    override fun handleIntent() {
        TODO("Not yet implemented")
    }

}