package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomListBinding
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragment : MainFragment() {
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
            override fun onJoinChatroomButtonPress(view: View, chatroom: Chatroom) {
                // TODO: If user join chatroom is successful, delete other chatrooms from cache (+remote keys) and load only their chatroom from cache
                // TODO: Send user update for chatroom Id and handle no connection and other errors

                lifecycleScope.launch {
                    activityViewModel.sendIntent(UserIntent.UpdateUser(mutableMapOf(
                        Pair(Constants.CHATROOM_ID, chatroom.id.toString())
                    )))
                }
            }
        })

        chatroomListAdapter.withLoadStateFooter(DefaultLoadStateAdapter({
                chatroomListAdapter.refresh()
            },
            object : DefaultLoadStateAdapter.OnCreateChatroomButtonPressed {
                override fun onCreateChatroomButtonPress(view: View) {
                    navController.navigate(R.id.action_chatroomListFragment_to_chatroomCreateActivity)
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

            lifecycleScope.launch {
                viewModel!!.mainState.collect {
                    when(it) {
                        is ChatroomListState.Idle -> {

                        }
                        is ChatroomListState.Loading -> {

                        }
                        is ChatroomListState.ChatroomsResult -> {
                            chatroomListAdapter.submitData(it.chatrooms)
                        }
                        is ChatroomListState.Error -> {
                            Toast.makeText(requireContext(), "Problem loading chatrooms! ${it.error}", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }

                if(viewModel!!.chatroomPagingData == null) {
                    viewModel!!.sendIntent(ChatroomListIntent.FetchChatrooms(
                        activityViewModel.authUser.value?.chatroomId != null
                        )
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        (chatroomList.adapter as ChatroomListAdapter).submitData(viewModel!!.chatroomPagingData!!)
                    }
                }
            }

            return@onCreateView root
        }
    }
}