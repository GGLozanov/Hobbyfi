package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hobbyfi.R

class ChatroomListFragment : MainFragment() {
    // TODO: Refresh chatroom callback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatroom_list, container, false)

        return view
    }
}