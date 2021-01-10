package com.example.hobbyfi.ui.main

import android.app.AlertDialog
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
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.ChatroomListState
import com.example.hobbyfi.state.State
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
class ChatroomListFragment : MainListFragment<ChatroomListAdapter>() {
    override val chatroomListAdapter: ChatroomListAdapter = ChatroomListAdapter(onChatroomJoinButton)

    override fun observeChatroomEntryState() {
        activityViewModel.joinedChatroom.observe(viewLifecycleOwner, Observer { joined ->
            fun joinChatroomAndUpdate() {
                joinChatroom()
                activityViewModel.updateUserWithLatestFields()
            }

            if(joined) {
                // this check is required for whenever this observer might be trigger by CreateChatroomFragment
                if(viewModel.buttonSelectedChatroom != null) {
                    FirebaseMessaging.getInstance().subscribeToTopic(Constants.chatroomTopic(
                        viewModel.buttonSelectedChatroom!!.id)
                    ).addOnCompleteListener {
                        joinChatroomAndUpdate()
                        // TODO: Add dialogfragment for accepting/denying navigation to chatroom (+"Don't remind me again" in sharedprefs)
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
    }

    override fun observeAuthUser() {
        activityViewModel.authUser.observe(viewLifecycleOwner, Observer { user ->
            if(user != null) {
                Log.i("ChatroomListFragment", "user chatroom ids: ${user.chatroomIds}")

                // TODO: Modify for support for one-to-many connection AND modify DeleteChatroomsCache
                // TODO: To delete chatrooms with IDs IN user chatroom IDs array
                val userHasChatroom = user.chatroomIds != null

                loadStateAdapter?.setUserChatroomOwnership(userHasChatroom)

                lifecycleScope.launch {
                    // TODO: Add condition if chatroom ids have changed
                    if(chatroomListAdapter.itemCount == 0) {
                        viewModel.sendIntent(ChatroomListIntent.FetchChatrooms(user.chatroomIds))
                    }

                    observeChatroomsState()
                }
            }
        })
    }

    override suspend fun observeChatroomsState() {
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

                            chatroomListAdapter.submitData(data.filterSync {
                                return@filterSync activityViewModel.authUser.value!!.chatroomIds?.contains(it.id) == false
                            })
                        }
                    }
                }
                is ChatroomListState.Error -> {
                    if(state.shouldReauth) {
                        (requireActivity() as MainActivity).logout()
                    }

                    Toast.makeText(requireContext(), state.error, Toast.LENGTH_LONG)
                        .show()
                }
                else -> throw State.InvalidStateException()
            }
        }
    }


    // TODO: Probably remove `setCurrentChatrooms(null)`
    private fun joinChatroom() {
        viewModel.setCurrentChatrooms(null) // account for user owner of room
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }
}