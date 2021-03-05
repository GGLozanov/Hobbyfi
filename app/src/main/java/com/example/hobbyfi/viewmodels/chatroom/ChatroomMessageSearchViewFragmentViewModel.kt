package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomMessageSearchViewFragmentViewModel(
    application: Application
): ChatroomMessageViewModel(application) {

    init {
        handleIntent()
    }
}