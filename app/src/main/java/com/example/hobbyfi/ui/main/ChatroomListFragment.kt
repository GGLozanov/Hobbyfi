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
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomListBinding
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.findItemFromCurrentPagingData
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kodein.di.generic.instance
import java.lang.Exception

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
            FirebaseMessaging.getInstance().subscribeToTopic(Constants.chatroomTopic(userChatroomId))
                .addOnCompleteListener {
                    updateJob = lifecycleScope.launch {
                        navigateToChatroom()
                    }
            }.addOnFailureListener(fcmTopicErrorFallback) // subscribe (ex: after user logout)
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

    private var updateJob: Job? = null

    private val fcmTopicErrorFallback: OnFailureListener by instance(
        tag = "fcmTopicErrorFallback",
        MainApplication.applicationContext
    )


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
                    .collect { binding.swiperefresh.isRefreshing = false }
            }

            initChatroomListAdapter()

            observeChatroomState()
            observeAuthUser()
            observeConnectionRefresh()

            return@onCreateView root
        }
    }

    private fun observeChatroomState() {
        activityViewModel.joinedChatroom
            .observe(viewLifecycleOwner, Observer { joined ->
                fun joinChatroomAndUpdate() {
                    joinChatroom()
                    activityViewModel.updateUserWithLatestFields()
                }

                if(joined) {
                    // this check is required for whenever this observer might be trigger by CreateChatroomFragment
                    if(viewModel.buttonSelectedChatroom != null) {
                        // TODO: Handle variable network connection with this and all the other request with WorkManager
                        FirebaseMessaging.getInstance().subscribeToTopic(Constants.chatroomTopic(
                            viewModel.buttonSelectedChatroom!!.id)).addOnCompleteListener {
                            joinChatroomAndUpdate()
                            navigateToChatroom()
                        }.addOnFailureListener(fcmTopicErrorFallback)
                    } else {
                        joinChatroomAndUpdate()
                    }
                    activityViewModel.setJoinedChatroom(false)
                } else {
                    Log.i("ChatroomListFragment", "Observing user joined chatroom false")
                }
            })

        activityViewModel.leftChatroom
            .observe(viewLifecycleOwner, Observer { left ->
                fun leaveChatroomAndUpdate() {
                    leaveChatroom()
                    activityViewModel.updateUserWithLatestFields()
                }

                if(left) {
                    if(viewModel.buttonSelectedChatroom != null) {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.chatroomTopic(
                            viewModel.buttonSelectedChatroom!!.id)).addOnCompleteListener {
                            leaveChatroomAndUpdate()
                        }.addOnFailureListener(fcmTopicErrorFallback)
                    } else {
                        leaveChatroomAndUpdate()
                    }
                    activityViewModel.setLeftChatroom(false)
                }  else {
                    Log.i("ChatroomListFragment", "Observing user left chatroom false")
                }
            })
    }

    private fun observeAuthUser() {
        activityViewModel.authUser.observe(viewLifecycleOwner, Observer { user ->
            if(user != null) {
                Log.i("ChatroomListFragment", "user chatroom id: ${user.chatroomId}")

                val userHasChatroom = user.chatroomId != null

                loadStateAdapter?.setUserHasChatroom(userHasChatroom)
                setChatroomLeaveButtonVisibility(userHasChatroom) // possible performance issues here

                lifecycleScope.launch {
                    if(userHasChatroom && chatroomListAdapter.itemCount != 0) {
                        if(!viewModel.hasDeletedCacheForSession) {
                            viewModel.sendIntent(ChatroomListIntent.DeleteChatroomsCache(user.chatroomId!!))
                        }
                    } else {
                        viewModel.sendIntent(ChatroomListIntent.FetchChatrooms(userHasChatroom))
                    }

                    observeChatroomsState()
                }
            }
        })
    }

    private fun observeConnectionRefresh() {
        (requireActivity() as BaseActivity).refreshConnectivityMonitor.observe(viewLifecycleOwner, Observer { connectionRefreshed ->
            if(connectionRefreshed) {
                Log.i("ChatroomListFragment", "ChatroomListFragment CONNECTED")
                chatroomListAdapter.refresh()
            } else {
                Log.i("ChatroomListFragment", "ChatroomListFragment DIS-CONNECTED")
            }
        })
    }

    private suspend fun observeChatroomsState() {
        viewModel.mainState.collectLatest { state ->
            when(state) {
                is ChatroomListState.Idle -> {

                }
                is ChatroomListState.Loading -> {

                }
                is ChatroomListState.OnData.ChatroomsResult -> {
                    lifecycleScope.launch {
                        state.chatrooms.catch { e ->
                            e.printStackTrace()
                            if((e as Exception).isCritical) {
                                Toast.makeText(requireContext(), Constants.reauthError, Toast.LENGTH_LONG)
                                    .show()
                                (requireActivity() as MainActivity).logout()
                            } else if(e !is CancellationException) {
                                Log.i("ChatroomListFragment", "state.chatrooms collect() received a normal exception: $e")
                                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }.collectLatest { data ->
                            setChatroomLeaveButtonVisibility(state.isJustAuthChatroom) // possible performance issues here

                            chatroomListAdapter.submitData(data)
                        }
                    }
                }
                is ChatroomListState.OnData.DeleteChatroomsCacheResult -> {
                    Log.i("ChatroomListFragment", "Deleted chatrooms cache. User has a chatroom already: ${activityViewModel.authUser.value?.chatroomId}")
                }
                is ChatroomListState.Error -> {
                    if(state.shouldReauth) {
                        (requireActivity() as MainActivity).logout()
                    }

                    Toast.makeText(requireContext(), state.error, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun setChatroomLeaveButtonVisibility(userHasChatroom: Boolean) {
        // TODO: findItemFromCurrentPagingData uses method that generates list for current paging data each time
        // TODO: This might cause performance issues later on but solves bugs related to button visibility
        // TODO: Optimise the method
        val userNotOwner = chatroomListAdapter.findItemFromCurrentPagingData { it.id == activityViewModel.authUser.value?.chatroomId }
            ?.ownerId != activityViewModel.authUser.value!!.id
        chatroomListAdapter.setLeaveChatroomButtonVisibility(userHasChatroom && userNotOwner)
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
        viewModel.setHasDeletedCacheForSession(false)
        viewModel.setCurrentChatrooms(null) // account for user owner of room
    }

    private fun leaveChatroom() {
        viewModel.setHasDeletedCacheForSession(false)
        viewModel.setButtonSelectedChatroom(null)
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
        viewModel.setButtonSelectedChatroom(null)
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }
}