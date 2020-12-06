package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomListBinding
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.withExpandedLoadStateFooter
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragment : MainFragment() {
    // TODO: Refresh chatroom callback with REFRESH in remoteMediator
    private val viewModel: ChatroomListFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomListBinding

    private var searchJob: Job? = null

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
                    viewModel.sendIntent(ChatroomListIntent.DeleteChatroomsCache(chatroom.id))
                }
            }
        })

        chatroomListAdapter.withLoadStateFooter(DefaultLoadStateAdapter({
                chatroomListAdapter.refresh()
            },
            object : DefaultLoadStateAdapter.OnCreateChatroomButtonPressed {
                override fun onCreateChatroomButtonPress(view: View) {
                    navController.navigate(R.id.action_global_activityChatroomCreate)
                }
            })
        )

        with(binding) {
            chatroomList.addItemDecoration(VerticalSpaceItemDecoration(20))
            chatroomList.adapter = chatroomListAdapter

            swiperefresh.setOnRefreshListener {
                (chatroomList.adapter as ChatroomListAdapter).refresh() // should trickle down to remote mediator and VM
            }

            Log.i("ChatroomListFragment", "Sending FetchChatrooms intent. User has a chatroom already: ${activityViewModel.authUser.value?.chatroomId}")

            lifecycleScope.launch {
                chatroomListAdapter.loadStateFlow
                    // Only emit when REFRESH LoadState for RemoteMediator changes.
                    .distinctUntilChangedBy { loadState -> loadState.refresh }
                    // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                    .filter { loadState -> loadState.refresh is LoadState.NotLoading }
                    .collect { chatroomList.scrollToPosition(0)
                    binding.swiperefresh.isRefreshing = false }
            }

            activityViewModel.authUser.observe(viewLifecycleOwner, Observer {
                if(it != null) {
                    lifecycleScope.launch {
                        val userHasChatroom = it.chatroomId != null

                        if(userHasChatroom) {
                            viewModel!!.sendIntent(ChatroomListIntent.DeleteChatroomsCache(it.chatroomId!!))
                        }

                        viewModel!!.sendIntent(ChatroomListIntent.FetchChatrooms(
                                userHasChatroom
                            )
                        )

                        chatroomListAdapter.setLeaveChatroomButtonVisibility(
                            userHasChatroom
                        )

                        viewModel!!.mainState.collectLatest { state ->
                            when(state) {
                                is ChatroomListState.Idle -> {

                                }
                                is ChatroomListState.Loading -> {

                                }
                                is ChatroomListState.ChatroomsResult -> {
                                    binding.swiperefresh.isRefreshing = false
                                    searchJob = lifecycleScope.launch {
                                        state.chatrooms.catch { e ->
                                            e.printStackTrace()
                                            if(e is Repository.ReauthenticationException) {
                                                Toast.makeText(requireContext(), Constants.reauthError, Toast.LENGTH_LONG)
                                                    .show()
                                                // TODO: Handle logout through interface
                                            } else  {
                                                Log.i("ChatroomListFragment", "state.chatrooms collect() received a normal exception: $e")
                                                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
                                                    .show()
                                            }
                                        }.collectLatest { data ->
                                            chatroomListAdapter.submitData(data)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            })

            return@onCreateView root
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel() // cancel job once we need to yeet from the fragment
    }
}