package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomListBinding
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomListFragment : MainFragment(),
    DefaultLoadStateAdapter.OnCreateChatroomButtonPressed, ChatroomListAdapter.OnJoinChatroomButtonPressed {
    // TODO: Refresh chatroom callback with REFRESH in remoteMediator
    private val viewModel: ChatroomListFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_chatroom_list,
            container, false
        )

        binding.viewModel = viewModel

        val chatroomListAdapter = ChatroomListAdapter(object : ChatroomListAdapter.OnJoinChatroomButtonPressed {
            override fun onJoinChatroomButtonPress(view: View) {
                // TODO: If user join chatroom is successful, delete other chatrooms from cache (+remote keys) and load only their chatroom from cache
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

        with(binding) {
            chatroomList.addItemDecoration(VerticalSpaceItemDecoration(20))
            chatroomList.adapter = chatroomListAdapter

            swiperefresh.setOnRefreshListener {
                (chatroomList.adapter as ChatroomListAdapter).refresh()
                // should trickle down to remote mediator and VM
            }

            return@onCreateView root
        }
    }

    override fun onCreateChatroomButtonPress(view: View) {
        TODO("Not yet implemented")
    }

    override fun onJoinChatroomButtonPress(view: View) {
        TODO("Not yet implemented")
    }
}