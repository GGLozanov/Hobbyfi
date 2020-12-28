package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.MessageRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.state.MessageState
import com.example.hobbyfi.state.UserListState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import java.lang.Exception

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomMessageListFragmentViewModel(application: Application) :
    StateIntentViewModel<MessageListState, MessageListIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    private val messageRepository: MessageRepository by instance(tag = "messageRepository")
    private var currentMessages: Flow<PagingData<Message>>? = null

    val areCurrentMessagesNull get() = currentMessages == null

    @Bindable
    val message: MutableLiveData<String> = MutableLiveData()

    override val mainStateIntent: StateIntent<MessageListState, MessageListIntent> = object : StateIntent<MessageListState, MessageListIntent>() {
        override val _state: MutableStateFlow<MessageListState> = MutableStateFlow(MessageListState.Idle)
    }

    val messageStateIntent: StateIntent<MessageState, MessageIntent> = object : StateIntent<MessageState, MessageIntent>() {
        override val _state: MutableStateFlow<MessageState> = MutableStateFlow(MessageState.Idle)
    }

    val messageState get() = messageStateIntent.state

    suspend fun sendMessageIntent(intent: MessageIntent) {
        messageStateIntent.sendIntent(intent)
    }

    fun setCurrentMessages(messages: Flow<PagingData<Message>>?) {
        currentMessages = messages
    }

    override fun handleIntent() {
        viewModelScope.launch {
            // TODO: Initialise stateintents which are not main here because NPE
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is MessageListIntent.FetchMessages -> {
                        fetchMessages()
                    }
                    is MessageListIntent.DeleteMessagesCache -> {

                    }
                }
            }
        }
        viewModelScope.launch {
            messageStateIntent.intentAsFlow().collect {
                when(it) {

                }
            }
        }
    }

    init {
        handleIntent()
    }

    private suspend fun fetchMessages() {
        mainStateIntent.setState(MessageListState.Loading)

        if(currentMessages == null) {
            currentMessages = messageRepository.getMessages()
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
        }

        mainStateIntent.setState(MessageListState.MessagesResult(currentMessages!!))
    }

    private suspend fun createMessage(message: String) {
        messageStateIntent.setState(MessageState.Loading)

        messageStateIntent.setState(try {
            val state = MessageState.OnData.MessageCreateResult(
                messageRepository.createMessage(message)
            )

            // TODO: Save message in cache

            state
        } catch(ex: Exception) {
            MessageState.Error(
                ex.message,
                isExceptionCritical(ex)
            )
        })
    }

    private suspend fun updateMessage(id: Int) {
        messageStateIntent.setState(MessageState.Loading)

    }

    private suspend fun updateAndSaveMessage(message: Message) {

    }

    private suspend fun deleteMessage(id: Int) {
        messageStateIntent.setState(MessageState.Loading)

    }

    private suspend fun deleteMessageCache(id: Int) {

    }

    private suspend fun deleteMessagesCache() {
        var state: MessageListState = MessageListState.Error(Constants.cacheDeletionError)

        // deletes other cached chatrooms (not auth'd) for user
        if(viewModelScope.async {
                messageRepository.deleteMessagesCache()
            }.await()) {
            state = MessageListState.DeleteMessagesCacheResult
        }

        mainStateIntent.setState(state)
    }
}