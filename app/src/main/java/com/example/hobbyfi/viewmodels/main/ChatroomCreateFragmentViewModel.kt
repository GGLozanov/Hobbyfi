package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Tag
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

    private val _tags: MutableList<Tag> = Constants.predefinedTags.toMutableList()
    val tags: List<Tag> get() = _tags
    private var _selectedTags: List<Tag> = emptyList()
    // this livedata instance will have its value updated once the tag selection dialog fragment finishes its workTag
    val selectedTags: List<Tag> get() = _selectedTags

    // FIXME: Code dup with RegisterFragmentViewModel. . .
    fun appendNewSelectedTagsToTags(selectedTags: List<Tag>) {
        _tags.appendNewSelectedTagsToTags(selectedTags)
    }

    fun setSelectedTags(selectedTags: List<Tag>) {
        _selectedTags = selectedTags
    }


    private var _base64Image: String? = null
    val base64Image get() = _base64Image

    fun setProfileImageBase64(base64Image: String) {
        _base64Image = base64Image
    }


    init {
        handleIntent()
    }

    override val _mainState: MutableStateFlow<ChatroomState> = MutableStateFlow(ChatroomState.Idle)

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collect {
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
        _mainState.value = ChatroomState.Loading
        _mainState.value = try {
            ChatroomState.OnData.ChatroomCreateResult(
                chatroomRepository.createChatroom(
                    name.value!!,
                    description.value!!,
                    base64Image,
                    selectedTags
                )
            )
        } catch(ex: Exception) {
            ex.printStackTrace()
            ChatroomState.Error(
                ex.message,
                ex is Repository.ReauthenticationException
            )
        }
    }
}
