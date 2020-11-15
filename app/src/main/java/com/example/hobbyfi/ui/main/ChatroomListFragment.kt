package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadStateAdapter
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.LoaderStateAdapter
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter

class ChatroomListFragment : MainFragment() {
    // TODO: Refresh chatroom callback with REFRESH in remoteMediator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatroom_list, container, false)

        val chatroomListAdapter = ChatroomListAdapter()
        // chatroomListAdapter.refresh()
//        chatroomListAdapter.withLoadStateFooter(LoaderStateAdapter {
//
//        })

        // TODO: Handle expired token error & logout

        return view
    }
}