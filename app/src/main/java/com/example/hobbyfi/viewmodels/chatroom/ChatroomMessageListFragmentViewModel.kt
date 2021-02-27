package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.MessageRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.state.MessageState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kodein.di.generic.instance
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