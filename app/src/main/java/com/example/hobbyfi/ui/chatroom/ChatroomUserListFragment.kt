package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.hobbyfi.R

class ChatroomUserListFragment : ChatroomFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatroom_user_list, container, false)

        // TODO: Handle expired token error & logout

        // TODO: On right navdrawer press (through activity listener), refresh users data source in viewmodel and fetch new users
        //  => triggers REFRESH loadstate in Mediator => check if users last fetch time is too long
        //  => doesn't delete old users (if no connection or time isn't long enough) => fetches users if time is long enough
        // TODO: Invalidate users data source upon notification & refetch from network

        return view
    }

}