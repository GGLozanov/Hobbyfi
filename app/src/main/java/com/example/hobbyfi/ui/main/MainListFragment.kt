package com.example.hobbyfi.ui.main

import com.example.hobbyfi.adapters.chatroom.BaseChatroomListAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class MainListFragment<T: BaseChatroomListAdapter<*>> : MainFragment() {
    // TODO: Add common list fragment functionality here
    protected abstract val chatroomListAdapter: T

    protected abstract fun observeAuthUser()
}