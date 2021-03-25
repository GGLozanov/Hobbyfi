package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.repositories.MessageRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
abstract class ChatroomMessageViewModel(
    application: Application
): StateIntentViewModel<MessageListState, MessageListIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    protected val messageRepository: MessageRepository by instance(tag = "messageRepository")
    protected var _currentMessages: Flow<PagingData<UIMessage>>? = null

    protected var _sentMessageIdFetchRequestPrior: Boolean = false
    val sentMessageIdFetchRequestPrior get() = _sentMessageIdFetchRequestPrior
    fun setSentMessageIdFetchRequestPrior(sent: Boolean) {
        _sentMessageIdFetchRequestPrior = sent
    }

    fun setCurrentMessages(messages: Flow<PagingData<UIMessage>>?) {
        _currentMessages = messages
    }

    val currentMessages: Flow<PagingData<UIMessage>>? get() = _currentMessages

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
        if(_currentMessages == null || messageId != null || _sentMessageIdFetchRequestPrior) {
            _currentMessages = (if(query == null)
                        messageRepository.getMessages(chatroomId = chatroomId, messageId = messageId)
                                else messageRepository.getSearchMessages(chatroomId = chatroomId, query = query))
                .distinctUntilChanged()
                .map { pagingData ->
                    pagingData.map { message -> UIMessage.MessageItem(message) }
                }
                .map {
                    it.insertSeparators { before, after ->
                        if (after == null) {
                            // end of the list
                            return@insertSeparators null
                        }

                        if (before == null) {
                            // beginning of the list
                            return@insertSeparators null
                        }

                        // check between 2 items
                        val beforeTime = LocalDateTime.parse(before.message.createTime.replace(' ', 'T'))
                            .toLocalDate()
                        val afterTime = LocalDateTime.parse(after.message.createTime.replace(' ', 'T'))
                            .toLocalDate()

                        when {
                            abs(ChronoUnit.DAYS.between(beforeTime, afterTime)) > 0 -> {
                                val now = LocalDateTime.now()
                                val dayDiffAfterNow = abs(ChronoUnit.DAYS.between(afterTime, now))
                                val dayDiffBeforeNow = abs(ChronoUnit.DAYS.between(beforeTime, now))

                                when {
                                    dayDiffAfterNow == 1L &&
                                            dayDiffBeforeNow == 0L -> {
                                        // today
                                        UIMessage.MessageSeparatorItem("Today")
                                    }
                                    dayDiffAfterNow == 2L &&
                                            dayDiffBeforeNow == 1L -> {
                                        // yesterday
                                        UIMessage.MessageSeparatorItem("Yesterday")
                                    }
                                    // different date separator
                                    else -> UIMessage.MessageSeparatorItem(beforeTime.toString())
                                }
                            }
                            else -> null // no separator
                        }
                    }
                }
                .map {
                    it.insertHeaderItem(item = UIMessage.MessageUsersTypingItem(listOf()))
                }
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(MessageListState.OnData.MessagesResult(_currentMessages!!, messageId))
    }
}