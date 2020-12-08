package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
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
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragment : MainFragment() {
    // TODO: Refresh chatroom callback with REFRESH in remoteMediator
    val viewModel: ChatroomListFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomListBinding
    private lateinit var chatroomListAdapter: ChatroomListAdapter

    private var searchJob: Job? = null
    private var updateJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_chatroom_list,
            container, false
        )

        binding.viewModel = viewModel

        chatroomListAdapter = ChatroomListAdapter({ _, chatroom ->
                viewModel.setButtonSelectedChatroom(chatroom)
                val userChatroomId = activityViewModel.authUser.value?.chatroomId

                // if user does not have a chatroom
                if(userChatroomId == null || userChatroomId != chatroom.id) {
                    // If user join chatroom is successful, delete other chatrooms from cache (+remote keys) and load only their chatroom from cache
                    lifecycleScope.launch {
                        activityViewModel.sendIntent(UserIntent.UpdateUser(mutableMapOf(
                            Pair(Constants.CHATROOM_ID, chatroom.id.toString())
                        )))
                    }
                } else {
                    // otherwise simply allow the user to join their chatroom
                    updateJob = lifecycleScope.launch {
                        joinChatroom()
                    }
                }
            }, {_, chatroom ->
            viewModel.setButtonSelectedChatroom(chatroom)
            lifecycleScope.launch {
                activityViewModel.sendIntent(UserIntent.UpdateUser(mutableMapOf(
                    Pair(Constants.CHATROOM_ID, "0")
                )))
            }
        })

        with(binding) {
            chatroomListAdapter.withLoadStateFooter(DefaultLoadStateAdapter({
                chatroomListAdapter.retry()
            }, {
                navController.navigate(ChatroomListFragmentDirections.actionChatroomListFragmentToChatroomCreateNavGraph(
                    activityViewModel.authUser.value!!
                ))
            }))

            lifecycleScope.launch {
                chatroomListAdapter.loadStateFlow
                    // Only emit when REFRESH LoadState for RemoteMediator changes.
                    .distinctUntilChangedBy { loadState -> loadState.refresh }
                    // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                    .filter { loadState -> loadState.refresh is LoadState.NotLoading }
                    .collect { chatroomList.scrollToPosition(0)
                    binding.swiperefresh.isRefreshing = false }
            }

            // initial value chatroom id check for adapter init
            initChatroomListAdapter()

            activityViewModel.authUser.observe(viewLifecycleOwner, Observer { user ->
                if(user != null) {
                    lifecycleScope.launch {
                        Log.i("ChatroomListFragment", "user chatroom id: ${user.chatroomId}")
                        val userHasChatroom = user.chatroomId != null

                        if(userHasChatroom && !viewModel!!.hasDeletedCacheForSession) {
                            viewModel!!.sendIntent(ChatroomListIntent.DeleteChatroomsCache(user.chatroomId!!))
                        } else {
                            viewModel!!.sendIntent(ChatroomListIntent.FetchChatrooms(false))
                        }

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
                                    try {
                                        searchJob = lifecycleScope.launch {
                                            state.chatrooms.catch { e ->
                                                e.printStackTrace()
                                                if(e is Repository.ReauthenticationException) {
                                                    Toast.makeText(requireContext(), Constants.reauthError, Toast.LENGTH_LONG)
                                                        .show()
                                                } else  {
                                                    Log.i("ChatroomListFragment", "state.chatrooms collect() received a normal exception: $e")
                                                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
                                                        .show()
                                                }
                                            }.collectLatest { data ->
                                                ensureActive()
                                                chatroomListAdapter.submitData(data)
                                            }
                                        }
                                    } catch(ex: Exception) {
                                        ex.printStackTrace()
                                        Toast.makeText(requireContext(), ex.message, Toast.LENGTH_LONG)
                                            .show()
                                    }
                                }
                                is ChatroomListState.DeleteChatroomsCacheResult -> {
                                    Log.i("ChatroomListFragment", "Deleted chatrooms cache. User has a chatroom already: ${activityViewModel.authUser.value?.chatroomId}")
                                    if(!state.calledFromChatroomJoin) {
                                        Log.i("ChatroomListFragment", "Sending FetchChatrooms intent. User has a chatroom already: ${activityViewModel.authUser.value?.chatroomId}")

                                        viewModel!!.sendIntent(ChatroomListIntent.FetchChatrooms(
                                                userHasChatroom
                                            )
                                        )
                                    }
                                }
                                is ChatroomListState.Error -> {
                                    Toast.makeText(requireContext(), state.error, Toast.LENGTH_LONG)
                                        .show()
                                }
                            }
                        }
                    }
                }
            })
            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(Constants.chatroomJoined)
            ?.observe(viewLifecycleOwner, Observer { joined ->
                if(joined) {
                    updateJob = lifecycleScope.launch {
                        joinChatroom()
                    }
                } else {
                    Log.i("ChatroomListFragment", "Observing user joined chatroom false")
                }
            })

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(Constants.chatroomLeft)
            ?.observe(viewLifecycleOwner, Observer { left ->
                if(left) {
                    leaveChatroom()
                }  else {
                    Log.i("ChatroomListFragment", "Observing user joined chatroom false")
                }
            })
    }

    private fun initChatroomListAdapter() {
        with(binding) {
            chatroomList.addItemDecoration(VerticalSpaceItemDecoration(20))
            chatroomList.adapter = chatroomListAdapter

            swiperefresh.setOnRefreshListener {
                // TODO: Paging 3 bug currently whenever endOfPagination = true in refresh() https://issuetracker.google.com/issues/174769547
                // TODO: Attempted hack to fix it by unsubscribing from flow while the bug is fixed; HACK DOESN'T WORK AT THIS POINT
                searchJob?.cancel()
                chatroomListAdapter.refresh()
                // should trickle down to remote mediator and VM
            }
        }
    }

    private suspend fun joinChatroom() {
         viewModel.sendIntent(ChatroomListIntent.DeleteChatroomsCache(viewModel.buttonSelectedChatroom!!.id, calledFromChatroomJoin = true))

        navController.navigate(
            ChatroomListFragmentDirections.actionGlobalActivityChatroom(
                activityViewModel.authUser.value,
                viewModel.buttonSelectedChatroom,
            )
        )
        viewModel.setButtonSelectedChatroom(null)
    }

    private fun leaveChatroom() {
        viewModel.setButtonSelectedChatroom(null)
    }

    override fun onDestroyView() {
        searchJob?.cancel() // cancel job once we need to yeet from the fragment
        updateJob?.cancel()
        super.onDestroyView()
    }
}