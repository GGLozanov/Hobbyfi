package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.NavigationUI
import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomModelFragment : ChatroomFragment() {

    // TODO: Refactor with navdestinationchanged for navcontroller
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            (requireActivity() as ChatroomActivity)
                .binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } catch(e: Exception) {
            Log.i("ChatroomMFragment", "Call on requireActivity() w/ cast to ChatroomActivity to different Activity")
        }
        // hacky way to fix drawer but... so is life. Use toolbars and navviews on individual fragments, kids!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @ExperimentalCoroutinesApi
    override fun onDestroyView() {
        super.onDestroyView()
        try {
            (requireActivity() as ChatroomActivity)
                .binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } catch(e: Exception) {
            Log.i("ChatroomMFragment", "Call on requireActivity() w/ cast to ChatroomActivity to different Activity")
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }
}