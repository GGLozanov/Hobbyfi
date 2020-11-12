package com.example.hobbyfi.ui.chatroom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavDestination
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_chatroom.*

class ChatroomActivity : BaseActivity() {
    // TODO: Handle navdrawer, rendering of fragments, activity intent calls, button listeners, etc. . .
    // TODO: Have user & chatroom info passed in (or fetched from cache) & send request for messages, users, & event
    // TODO: Disable event create button if event is already created

    // TODO: research into integrating nav drawer different icons with navcomponent
    // TODO: need to integrate action bar menu and have drawable from there trigger right navigationview
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom)
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawer_layout)
        nav_view_chatroom.setupWithNavController(navController)

        // TODO: if(user_is_admin) - viewmodel & databinding
        nav_view_admin.setupWithNavController(navController)

    }
}