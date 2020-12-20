package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomModelFragment : ChatroomFragment() {

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as ChatroomActivity)
            .binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        // hacky way to fix drawer but... so is life. Use toolbars and navviews on individual fragemnts, kids!
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }
}