package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomCreateFragmentViewModel(application: Application, initialTags: List<Tag>) : StateIntentViewModel<ChatroomState, ChatroomIntent>(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {

    // TODO: Add tags (try without code dup)

    init {
        handleIntent()
    }

    override val _mainState: MutableStateFlow<ChatroomState> = MutableStateFlow(ChatroomState.Idle)

    override fun handleIntent() {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collect {
                when(it) {
                    is ChatroomIntent.CreateChatroom -> {

                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private fun createChatroom() {


    }
}
