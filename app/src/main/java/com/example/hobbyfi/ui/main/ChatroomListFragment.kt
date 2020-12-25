package com.example.hobbyfi.ui.main

import android.content.Context
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
import com.example.hobbyfi.MainApplication
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
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kodein.di.generic.instance
import org.kodein.di.generic.on

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragment : MainFragment() {
    private val viewModel: ChatroomListFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomListBinding
    private val chatroomListAdapter: ChatroomListAdapter = ChatroomListAdapter({ _, chatroom ->
        viewModel.setButtonSelectedChatroom(chatroom)
        val userChatroomId = activityViewModel.authUser.value?.chatroomId

        // if user does not have a chatroom
        if(userChatroomId == null || userChatroomId != chatroom.id) {
            // If user join chatroom is successful, delete other chatrooms from cache (+remote keys) and load only their chatroom from cache
            lifecycleScope.launch {
                activityViewModel.sendIntent(UserIntent.UpdateUser(mapOf(
                    Pair(Constants.CHATROOM_ID, chatroom.id.toString())
                )))
            }
        } else {
            // otherwise simply allow the user to join their chatroom
            updateJob = lifecycleScope.launch {
                navigateToChatroom()
            }
        }
    }, {_, chatroom ->
        viewModel.setButtonSelectedChatroom(chatroom)
        lifecycleScope.launch {
            activityViewModel.sendIntent(UserIntent.UpdateUser(mapOf(
                Pair(Constants.CHATROOM_ID, "0")
            )))
        }
    })
    private var loadStateAdapter: DefaultLoadStateAdapter? = null

    private var searchJob: Job? = null
    private var updateJob: Job? = null

    private val fcmTopicErrorFallback: OnFailureListener by instance(
        tag = "fcmTopicErrorFallback", MainApplication.applicationContext)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_chatroom_list,
            container, false
        )

        binding.viewModel = viewModel

        with(binding) {
           lifecycleScope.launch {
                chatroomListAdapter.loadStateFlow
                    // Only emit when REFRESH LoadState for RemoteMediator changes.
                    .distinctUntilChangedBy { loadState -> loadState.refresh }
                    // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                    .filter { loadState -> loadState.refresh is LoadState.NotLoading }
                    .collect { chatroomList.scrollToPosition(0)
                    binding.swiperefresh.isRefreshing = false }
            }

            initChatroomListAdapter()

            observeChatroomState()
            observeAuthUser()

            return@onCreateView root
        }
    }

    private fun observeChatroomState() {
        activityViewModel.joinedChatroom
            .observe(viewLifecycleOwner, Observer { joined ->
                if(joined) {
                    updateJob = lifecycleScope.launch {
                        // TODO: Handle variable network connection with this and all the other request with WorkManager
                        FirebaseMessaging.getInstance().subscribeToTopic(Constants.chatroomTopic(
                            viewModel.buttonSelectedChatroom!!.id)).addOnCompleteListener {
                            joinChatroom()
                        }.addOnFailureListener(fcmTopicErrorFallback)
                    }
                } else {
                    Log.i("ChatroomListFragment", "Observing user joined chatroom false")
                }
            })

        activityViewModel.leftChatroom
            .observe(viewLifecycleOwner, Observer { left ->
                if(left) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.chatroomTopic(
                        viewModel.buttonSelectedChatroom!!.id)).addOnCompleteListener {
                        leaveChatroom()
                    }.addOnFailureListener(fcmTopicErrorFallback)
                }  else {
                    Log.i("ChatroomListFragment", "Observing user left chatroom false")
                }
            })
    }

    private fun observeAuthUser() {
        activityViewModel.authUser.observe(viewLifecycleOwner, Observer { user ->
            if(user != null) {
                lifecycleScope.launch {
                    Log.i("ChatroomListFragment", "user chatroom id: ${user.chatroomId}")

                    val userHasChatroom = user.chatroomId != null

                    loadStateAdapter?.setUserHasChatroom(userHasChatroom)

                    if(userHasChatroom) {
                        viewModel.sendIntent(ChatroomListIntent.DeleteChatroomsCache(user.chatroomId!!))
                    } else {
                        viewModel.sendIntent(ChatroomListIntent.FetchChatrooms(userHasChatroom))
                    }

                    viewModel.mainState.collectLatest { state ->
                        when(state) {
                            is ChatroomListState.Idle -> {

                            }
                            is ChatroomListState.Loading -> {

                            }
                            is ChatroomListState.ChatroomsResult -> {
                                binding.swiperefresh.isRefreshing = false
                                lifecycleScope.launch {
                                    state.chatrooms.catch { e ->
                                        e.printStackTrace()
                                        if(e is Repository.ReauthenticationException) {
                                            Toast.makeText(requireContext(), Constants.reauthError, Toast.LENGTH_LONG)
                                                .show()
                                            (requireActivity() as MainActivity).logout()
                                        } else if(e !is CancellationException) {
                                            Log.i("ChatroomListFragment", "state.chatrooms collect() received a normal exception: $e")
                                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
                                                .show()
                                        }
                                    }.collectLatest { data ->
                                        // can't use user.hasChatroom here because it differs in the different `collectLatest`-s
                                        setChatroomLeaveButtonVisibility(state.isJustAuthChatroom, user.id)  // account for user owner of room

                                        chatroomListAdapter.submitData(data)
                                    }
                                }
                            }
                            is ChatroomListState.DeleteChatroomsCacheResult -> {
                                Log.i("ChatroomListFragment", "Deleted chatrooms cache. User has a chatroom already: ${activityViewModel.authUser.value?.chatroomId}")

                                if(activityViewModel.joinedChatroom.value == true) {
                                    Log.i("ChatroomListFragment", "User joined chatroom: Navigating to chatroom!")
                                    navigateToChatroom()
                                    activityViewModel.setJoinedChatroom(false)

                                } else {
                                    viewModel.sendIntent(ChatroomListIntent.FetchChatrooms(userHasChatroom))
                                }
                            }
                            is ChatroomListState.Error -> {
                                if(state.error == Constants.cacheDeletionError && viewModel.hasDeletedCacheForSession) {
                                    return@collectLatest
                                }

                                Toast.makeText(requireContext(), state.error, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun initChatroomListAdapter() {
        with(binding) {
            chatroomList.addItemDecoration(VerticalSpaceItemDecoration(20))
            loadStateAdapter = DefaultLoadStateAdapter({
                chatroomListAdapter.retry()
            }, {
                navController.navigate(ChatroomListFragmentDirections.actionChatroomListFragmentToChatroomCreateFragment(
                    activityViewModel.authUser.value!!
                ))
            })

            chatroomList.adapter = chatroomListAdapter.withLoadStateFooter(loadStateAdapter!!)

            swiperefresh.setOnRefreshListener {
                // TODO: Paging 3 bug currently whenever endOfPagination = true in refresh() https://issuetracker.google.com/issues/174769547
                // TODO: Attempted hack to fix it by unsubscribing from flow while the bug is fixed; HACK DOESN'T WORK AT THIS POINT
                chatroomListAdapter.refresh()
                // should trickle down to remote mediator and VM
            }
        }
    }

    private fun joinChatroom() {
        viewModel.setCurrentChatrooms(null) // reinit list trigger. . .
    }

    private fun leaveChatroom() {
        viewModel.setCurrentChatrooms(null)
    }

    private fun navigateToChatroom() {
        // only called while user is currently joining a chatroom
        Log.i("ChatroomListFragment", "Navigating to ChatroomActivity")
        navController.navigate(
            ChatroomListFragmentDirections.actionChatroomListFragmentToChatroomActivity(
                activityViewModel.authUser.value,
                viewModel.buttonSelectedChatroom,
            )
        )
    }

    private fun setChatroomLeaveButtonVisibility(userHasChatroom: Boolean, userId: Long) {
        var userNotOwner = false
        try {
            userNotOwner = chatroomListAdapter.peek(0)?.ownerId != userId
        } catch(ex: IndexOutOfBoundsException) {
            Log.i("ChatroomListFragment", "Index out of bounds for current unloaded chatrooms => not setting chatroom visibility")
        }

        Log.i("ChatroomListFragment", "USER IS NOT OWNER??? ${userNotOwner}")
        Log.i("ChatroomListFragment", "USER HAS CHATROOM??? ${userHasChatroom}")

        chatroomListAdapter.setLeaveChatroomButtonVisibility(
            userHasChatroom && userNotOwner
        )
    }

    override fun onDestroy() {
        searchJob?.cancel() // cancel job once we need to yeet from the fragment
        updateJob?.cancel()
        super.onDestroy()
    }
}