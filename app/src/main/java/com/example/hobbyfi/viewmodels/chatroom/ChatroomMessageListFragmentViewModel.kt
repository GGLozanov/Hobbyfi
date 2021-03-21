package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.state.MessageState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.Exception

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomMessageListFragmentViewModel(
    application: Application
): ChatroomMessageViewModel(application) {
    @Bindable
    val message: PredicateMutableLiveData<String> = PredicateMutableLiveData { it == null ||
        it.isEmpty() || it.length >= 200
    }

    val messageStateIntent: StateIntent<MessageState, MessageIntent> = object : StateIntent<MessageState, MessageIntent>() {
        override val _state: MutableStateFlow<MessageState> = MutableStateFlow(MessageState.Idle)
    }
    val messageState get() = messageStateIntent.state

    fun resetMessageState() = messageStateIntent.setState(MessageState.Idle)

    suspend fun sendMessageIntent(intent: MessageIntent) {
        messageStateIntent.sendIntent(intent)
    }

    private val searchDeferredMessages: MutableList<Message> = mutableListOf()

    fun addSearchDeferredMessage(message: Message) {
        searchDeferredMessages.add(message)
    }

    fun createSearchDeferredMessages() {
        searchDeferredMessages.forEach {
            viewModelScope.launch {
                messageStateIntent.sendIntent(
                    MessageIntent.CreateMessageCache(
                        it
                    )
                )
            }
        }
        clearSearchDeferredMessages()
    }

    private fun clearSearchDeferredMessages() {
        searchDeferredMessages.clear()
    }

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            messageStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is MessageIntent.CreateMessage -> {
                        viewModelScope.launch {
                            createMessage(
                                it.message ?: message.value!!,
                                it.userSentId,
                                it.chatroomSentId
                            )
                        }
                    }
                    is MessageIntent.CreateMessageImages -> {
                        it.base64s.forEach { base64Mesage ->
                            viewModelScope.launch {
                                createMessage(
                                    base64Mesage,
                                    it.userSentId,
                                    it.chatroomSentId,
                                    true
                                )
                            }
                        }
                    }
                    is MessageIntent.CreateMessageCache -> {
                        saveNewMessage(it.message)
                    }
                    is MessageIntent.UpdateMessage -> {
                        viewModelScope.launch {
                            updateMessage(it.messageUpdateFields)
                        }
                    }
                    is MessageIntent.UpdateMessageCache -> {
                        updateAndSaveMessage(it.messageUpdateFields)
                    }
                    is MessageIntent.DeleteMessage -> {
                        viewModelScope.launch {
                            deleteMessage(it.messageId)
                        }
                    }
                    is MessageIntent.DeleteMessageCache -> {
                        deleteMessageCache(it.messageId)
                    }
                }
            }
        }
    }

    init {
        handleIntent()
    }

    private suspend fun createMessage(message: String, userSentId: Long,
                                      chatroomSentId: Long, imageMessage: Boolean = false) {
        messageStateIntent.setState(MessageState.Loading)

        messageStateIntent.setState(
            try {
                val state = MessageState.OnData.MessageCreateResult(
                    messageRepository.createMessage(chatroomSentId, message, imageMessage)
                )

                saveNewMessage(
                    Message(
                        state.response!!.id,
                        if(imageMessage)
                            BuildConfig.BASE_URL + "uploads/" + Constants.chatroomMessagesProfileImageDir(
                                chatroomSentId
                            ) + "/" + state.response.id + ".jpg" else message,
                        state.response.createTime,
                        userSentId,
                        chatroomSentId
                    )
                )

                state
            } catch (ex: Exception) {
                MessageState.Error(
                    ex.message,
                    ex.isCritical
                )
            }
        )
    }

    private suspend fun updateMessage(messageUpdateFields: Map<String, String?>) {
        messageStateIntent.setState(MessageState.Loading)

        messageStateIntent.setState(try {
            val state = MessageState.OnData.MessageUpdateResult(
                messageRepository.editMessage(messageUpdateFields)
            )

            updateAndSaveMessage(messageUpdateFields)

            state
        } catch(ex: Exception) {
            MessageState.Error(
                ex.message,
                ex.isCritical
            )
        })
    }

    private suspend fun updateAndSaveMessage(messageUpdateFields: Map<String, String?>) {
        messageRepository.updateMessageCache((messageUpdateFields[Constants.ID] ?: error("Message ID must not be null in updateAndSaveMessage call!"))
            .toLong(), messageUpdateFields[Constants.MESSAGE] ?: error("Message message must not be null in updateAndSaveMessage call!")
        )
    }

    private suspend fun saveNewMessage(message: Message) {
        messageRepository.saveNewMessage(message)
    }

    private suspend fun deleteMessage(id: Long) {
        messageStateIntent.setState(MessageState.Loading)

        messageStateIntent.setState(try {
            val state = MessageState.OnData.MessageDeleteResult(
                messageRepository.deleteMessage(id)
            )

            deleteMessageCache(id)

            state
        } catch(ex: Exception) {
            MessageState.Error(
                ex.message,
                ex.isCritical
            )
        })
    }

    // code duuuuuuup because generics and blablabla go brr
    private suspend fun deleteMessageCache(id: Long) {
        messageRepository.deleteMessageCache(id)

        // DEAD CODE because this gave problems
//        if(setState) {
//            messageStateIntent.setState(if(success) MessageState.OnData.DeleteMessageCacheResult
//                else MessageState.Error(Constants.cacheDeletionError))
//        } else if(!success) {
//            throw Exception(Constants.cacheDeletionError)
//        }
    }

    // might not need this method because Room foreign keys & oncascade deletion
    // TODO: Use for cache cleanup
    private suspend fun deleteMessagesCache(chatroomId: Long) {
        var state: MessageListState = MessageListState.Error(Constants.cacheDeletionError)

        // deletes other cached chatrooms (not auth'd) for user
        if(viewModelScope.async {
                messageRepository.deleteMessagesCache(chatroomId)
            }.await()) {
            // state = MessageListState.OnData.DeleteMessagesCacheResult
        }

        mainStateIntent.setState(state)
    }

    override val combinedObserversInvalidity: LiveData<Boolean>
        get() = message.invalidity
}