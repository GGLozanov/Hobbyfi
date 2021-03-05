package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.models.data.TagBundle
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomCreateFragmentViewModel(application: Application) : StateIntentViewModel<ChatroomState, ChatroomIntent>(application),
        NameDescriptionBindable by NameDescriptionBindableViewModel(), Base64ImageHolder by Base64ImageHolderViewModel() {
    private val chatroomRepository: ChatroomRepository by instance(tag = "chatroomRepository")

    var tagBundle: TagBundle = TagBundle()

    override val mainStateIntent: StateIntent<ChatroomState, ChatroomIntent> = object : StateIntent<ChatroomState, ChatroomIntent>() {
        override val _state: MutableStateFlow<ChatroomState> = MutableStateFlow(ChatroomState.Idle)
    }

    init {
        handleIntent()
    }

    fun resetState() {
        mainStateIntent.setState(ChatroomState.Idle)
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is ChatroomIntent.CreateChatroom -> {
                        viewModelScope.launch { // img upload warranting another coroutine
                            createChatroom(it.ownerId)
                        }
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun createChatroom(ownerId: Long) {
        mainStateIntent.setState(ChatroomState.Loading)
        mainStateIntent.setState(try {
            val response = chatroomRepository.createChatroom(
                name.value!!,
                description.value!!,
                base64Image.base64,
                tagBundle.selectedTags
            )

            val chatroom = Chatroom(
                response!!.id,
                name.value!!,
                description.value,
                if(base64Image.base64 != null) BuildConfig.BASE_URL + "uploads/" + Constants.chatroomProfileImageDir(response.id)
                        + "/" + response.id + ".jpg" else null,
                if(tagBundle.selectedTags.isEmpty()) null else tagBundle.selectedTags,
                ownerId,
                null
            )

            saveChatroom(chatroom)

            ChatroomState.OnData.ChatroomCreateResult(
                chatroom
            )
        } catch(ex: Exception) {
            ex.printStackTrace()
            ChatroomState.Error(
                ex.message,
                shouldExit = ex.isCritical
            )
        })
    }

    private suspend fun saveChatroom(chatroom: Chatroom) = chatroomRepository.saveChatroom(chatroom)
}
