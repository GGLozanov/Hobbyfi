package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter

class ChatroomListFragment : MainFragment() {
    // TODO: Refresh chatroom callback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatroom_list, container, false)

        val chatroomListAdapter = ChatroomListAdapter()
        chatroomListAdapter.retry()

        // TODO: Handle expired token error & logout

        return view
    }
}