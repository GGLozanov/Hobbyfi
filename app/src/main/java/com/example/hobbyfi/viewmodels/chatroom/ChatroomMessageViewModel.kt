package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.MessageRepository
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
abstract class ChatroomMessageViewModel(
    application: Application
) : StateIntentViewModel<MessageListState, MessageListIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    protected val messageRepository: MessageRepository by instance(tag = "messageRepository")
    protected var _currentMessages: Flow<PagingData<Message>>? = null

    fun setCurrentMessages(messages: Flow<PagingData<Message>>?) {
        _currentMessages = messages
    }

    val currentMessages: Flow<PagingData<Message>>? get() = _currentMessages

    val areCurrentMessagesNull get() = _currentMessages == null

    override val mainStateIntent: StateIntent<MessageListState, MessageListIntent> = object : StateIntent<MessageListState, MessageListIntent>() {
        override val _state: MutableStateFlow<MessageListState> = MutableStateFlow(MessageListState.Idle)
    }

    @ExperimentalPagingApi
    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is MessageListIntent.FetchMessages -> {
                        fetchMessages(it.chatroomId, it.query)
                    }
                }
            }
        }
    }

    @ExperimentalPagingApi
    protected fun fetchMessages(chatroomId: Long, query: String?) {
        mainStateIntent.setState(MessageListState.Loading)

        if(_currentMessages == null) {
            _currentMessages = messageRepository.getMessages(chatroomId = chatroomId, query = query)
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(MessageListState.OnData.MessagesResult(_currentMessages!!))
    }
}