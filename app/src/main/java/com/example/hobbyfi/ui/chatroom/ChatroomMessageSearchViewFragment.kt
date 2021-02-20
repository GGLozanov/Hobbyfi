package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel


class ChatroomMessageSearchViewFragment : ChatroomMessageFragment() {

    // custom adapter for RV (takes pagingdata again but displays different cards)
    // and can filter through stuff
    // initially always empty but on searchview get injected data (?)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_message_search_view, container, false)
    }

    override fun observeMessagesState() {
        
    }
}