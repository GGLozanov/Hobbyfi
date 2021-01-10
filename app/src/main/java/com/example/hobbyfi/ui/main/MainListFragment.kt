package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.chatroom.BaseChatroomListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomListBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
abstract class MainListFragment<T: BaseChatroomListAdapter<*>> : MainFragment() {
    protected val viewModel: ChatroomListFragmentViewModel by viewModels()
    protected lateinit var binding: FragmentChatroomListBinding

    protected var updateJob: Job? = null

    protected val fcmTopicErrorFallback: OnFailureListener by instance(
        tag = "fcmTopicErrorFallback",
        MainApplication.applicationContext
    )

    protected val onChatroomJoinButton: (View, Chatroom) -> Unit = { _: View, chatroom: Chatroom ->
        viewModel.setButtonSelectedChatroom(chatroom)
        val userChatroomIds = activityViewModel.authUser.value?.chatroomIds

        // if user does not have a chatroom (kinda redundant check)
        if (userChatroomIds == null || !userChatroomIds.contains(chatroom.id)) {
            // If user join chatroom is successful, delete other chatrooms from cache (+remote keys) and load only their chatroom from cache
            lifecycleScope.launch {
                activityViewModel.sendIntent(
                    UserIntent.UpdateUser(
                        mapOf(
                            Pair(
                                Constants.CHATROOM_IDS, Constants.tagJsonConverter.toJson(
                                    activityViewModel.authUser.value!!.chatroomIds?.plus(chatroom.id)
                                )
                            )
                        )
                    )
                )
            }
        } else {
            // otherwise simply allow the user to join their chatroom
            FirebaseMessaging.getInstance().subscribeToTopic(Constants.chatroomTopic(chatroom.id))
                .addOnCompleteListener {
                    updateJob = lifecycleScope.launch {
                        navigateToChatroom()
                    }
                }.addOnFailureListener(fcmTopicErrorFallback) // subscribe (ex: after user logout)
        }
    }

    protected abstract val chatroomListAdapter: T

    protected abstract fun observeAuthUser()

    protected abstract suspend fun observeChatroomsState()

    protected abstract fun observeChatroomEntryState()

    protected var loadStateAdapter: DefaultLoadStateAdapter? = null

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

            observeChatroomEntryState()
            observeAuthUser()
            observeConnectionRefresh()

            return@onCreateView root
        }
    }

    private fun initChatroomListAdapter() {
        with(binding) {
            chatroomList.addItemDecoration(VerticalSpaceItemDecoration(20))
            loadStateAdapter = DefaultLoadStateAdapter({
                chatroomListAdapter.retry()
            }, {
                navController.navigate(
                    ChatroomListFragmentDirections.actionChatroomListFragmentToChatroomCreateNavGraph(
                    activityViewModel.authUser.value!!
                ))
            })

            chatroomList.adapter = chatroomListAdapter.withLoadStateFooter(loadStateAdapter!!)

            swiperefresh.setOnRefreshListener {
                chatroomListAdapter.refresh()
                // should trickle down to remote mediator and VM
            }
        }
    }

    protected fun navigateToChatroom() {
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

    private fun observeConnectionRefresh() {
        (requireActivity() as BaseActivity).refreshConnectivityMonitor.observe(viewLifecycleOwner, Observer { connectionRefreshed ->
            if(connectionRefreshed) {
                Log.i("MainListFragment", "MainListFragment CONNECTED")
                chatroomListAdapter.refresh()
            } else {
                Log.i("MainListFragment", "MainListFragment DIS-CONNECTED")
            }
        })
    }
}