package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.TagBundle
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.appendNewSelectedTagsToTags
import com.example.hobbyfi.shared.getNewSelectedTagsWithTags
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomCreateFragmentViewModel(application: Application) : StateIntentViewModel<ChatroomState, ChatroomIntent>(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {
    private val chatroomRepository: ChatroomRepository by instance(tag = "chatroomRepository")

    var tagBundle: TagBundle = TagBundle()

    private var _base64Image: String? = null
    val base64Image get() = _base64Image

    fun setProfileImageBase64(base64Image: String) {
        _base64Image = base64Image
    }

    override val mainStateIntent: StateIntent<ChatroomState, ChatroomIntent> = object : StateIntent<ChatroomState, ChatroomIntent>() {
        override val _state: MutableStateFlow<ChatroomState> = MutableStateFlow(ChatroomState.Idle)
    }

    init {
        handleIntent()
    }

    override fun handleIntent() {
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is ChatroomIntent.CreateChatroom -> {
                        createChatroom()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun createChatroom() {
        mainStateIntent.setState(ChatroomState.Loading)
        mainStateIntent.setState(try {
            ChatroomState.OnData.ChatroomCreateResult(
                chatroomRepository.createChatroom(
                    name.value!!,
                    description.value!!,
                    base64Image,
                    tagBundle.selectedTags
                )
            )
        } catch(ex: Exception) {
            ex.printStackTrace()
            ChatroomState.Error(
                ex.message,
                ex is Repository.ReauthenticationException
            )
        })
    }
}
