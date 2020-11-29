package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomListFragment : MainFragment(),
    DefaultLoadStateAdapter.OnCreateChatroomButtonPressed, ChatroomListAdapter.OnJoinChatroomButtonPressed {
    // TODO: Refresh chatroom callback with REFRESH in remoteMediator
    private val viewModel: ChatroomListFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatroom_list, container, false)



        val chatroomListAdapter = ChatroomListAdapter(object : ChatroomListAdapter.OnJoinChatroomButtonPressed {
            override fun onJoinChatroomButtonPress(view: View) {

            }
        })
        // chatroomListAdapter.refresh()
        chatroomListAdapter.withLoadStateFooter(DefaultLoadStateAdapter({

            },
            object : DefaultLoadStateAdapter.OnCreateChatroomButtonPressed {
                override fun onCreateChatroomButtonPress(view: View) {
                    TODO("Not yet implemented")
                }
            })
        )

        // TODO: Handle expired token error & logout

        return view
    }

    override fun onCreateChatroomButtonPress(view: View) {
        TODO("Not yet implemented")
    }

    override fun onJoinChatroomButtonPress(view: View) {
        TODO("Not yet implemented")
    }
}