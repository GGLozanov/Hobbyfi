package com.example.hobbyfi.ui.chatroom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.hobbyfi.R
import com.example.hobbyfi.ui.base.BaseActivity

class ChatroomActivity : BaseActivity() {
    // TODO: Handle navdrawer, rendering of fragments, activity intent calls, button listeners, etc. . .
    // TODO: Have user & chatroom info passed in (or fetched from cache) & send request for messages, users, & event
    // TODO: Disable event create button if event is already created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom)
    }
}