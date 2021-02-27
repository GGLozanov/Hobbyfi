package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.MessageRepository
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
abstract class ChatroomMessageViewModel(
    application: Application
): StateIntentViewModel<MessageListState, MessageListIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    protected val messageRepository: MessageRepository by instance(tag = "messageRepository")
    protected var _currentMessages: Flow<PagingData<Message>>? = null

    protected var _sentMessageIdFetchRequestPrior: Boolean = false
    val sentMessageIdFetchRequestPrior get() = _sentMessageIdFetchRequestPrior
    fun setSentMessageIdFetchRequestPrior(sent: Boolean) {
        _sentMessageIdFetchRequestPrior = sent
    }

    fun setCurrentMessages(messages: Flow<PagingData<Message>>?) {
        _currentMessages = messages
    }

    val currentMessages: Flow<PagingData<Message>>? get() = _currentMessages

    val areCurrentMessagesNull get() = _currentMessages == null

    override val mainStateIntent: StateIntent<MessageListState, MessageListIntent> = object : StateIntent<MessageListState, MessageListIntent>() {
        override val _state: MutableStateFlow<MessageListState> = MutableStateFlow(MessageListState.Idle)
    }

    fun resetMessageListState() = mainStateIntent.setState(MessageListState.Idle)

    @ExperimentalPagingApi
    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is MessageListIntent.FetchMessages -> {
                        fetchMessages(it.chatroomId, it.query, it.messageId)
                    }
                }
            }
        }
    }

    @ExperimentalPagingApi
    protected fun fetchMessages(chatroomId: Long, query: String?, messageId: Long?) {
        mainStateIntent.setState(MessageListState.Loading)

        if(_currentMessages == null || messageId != null || _sentMessageIdFetchRequestPrior) {
            if(_sentMessageIdFetchRequestPrior) {
                _sentMessageIdFetchRequestPrior = !_sentMessageIdFetchRequestPrior
            }

            _currentMessages = (if(query == null)
                        messageRepository.getMessages(chatroomId = chatroomId, messageId = messageId)
                                else messageRepository.getSearchMessages(chatroomId = chatroomId, query = query))
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(MessageListState.OnData.MessagesResult(_currentMessages!!, messageId))
    }
}