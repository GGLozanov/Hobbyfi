package com.example.hobbyfi.ui.chatroom

import androidx.fragment.app.viewModels
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomMessageFragment : ChatroomFragment() {
    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    protected val viewModel: ChatroomMessageListFragmentViewModel by viewModels()

    protected abstract fun observeMessagesState()
}