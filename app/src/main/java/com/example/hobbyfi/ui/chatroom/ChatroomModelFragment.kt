package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomModelFragment : ChatroomFragment() {

    @ExperimentalPagingApi
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as ChatroomActivity)
            .binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        // hacky way to fix drawer but... so is life. Use toolbars and navviews on individual fragemnts, kids!
    }

    @ExperimentalPagingApi
    @ExperimentalCoroutinesApi
    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as ChatroomActivity)
            .binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }
}