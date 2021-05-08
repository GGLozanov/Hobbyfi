package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.models.ui.UIMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomMessageSearchViewFragmentViewModel(
    application: Application
): ChatroomMessageViewModel(application) {

    override val listBeginningItem: UIMessage.MessageUsersTypingItem?
        get() = null

    init {
        handleIntent()
    }
}