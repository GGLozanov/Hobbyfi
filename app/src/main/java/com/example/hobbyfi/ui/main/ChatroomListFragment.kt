package com.example.hobbyfi.ui.main

import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.DialogNoRemindBinding
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.ChatroomListState
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragment : MainListFragment<ChatroomListAdapter>() {
    override val chatroomListAdapter: ChatroomListAdapter = ChatroomListAdapter(onChatroomJoinButton)

    override fun observeChatroomEntryState() {
        activityViewModel.joinedChatroom.observe(viewLifecycleOwner, Observer { joined ->
            fun joinChatroomAndUpdate() {
                activityViewModel.updateUserWithLatestFields()
            }

            if(joined) {
                // this check is required for whenever this observer might be trigger by CreateChatroomFragment
                if(viewModel.buttonSelectedChatroom != null) {
                    FirebaseMessaging.getInstance().subscribeToTopic(Constants.chatroomTopic(
                        viewModel.buttonSelectedChatroom!!.id)
                    ).addOnCompleteListener {
                        joinChatroomAndUpdate()
                        // TODO: Add clear pref option
                        when(prefConfig.readChatroomJoinRememberNavigate()) {
                            Constants.NoRememberDualChoice.NO_REMEMBER.ordinal -> {
                                val dialogBinding = DialogNoRemindBinding.inflate(layoutInflater)
                                AlertDialog.Builder(requireContext())
                                    .setView(dialogBinding.root)
                                    .setPositiveButton(Constants.takeMeThere) { dialogInterface: DialogInterface, _: Int ->
                                        prefConfig.writeChatroomJoinRememberNavigate(
                                            if(dialogBinding.checkBox.isChecked) Constants.NoRememberDualChoice.REMEMBER_YES.ordinal
                                            else Constants.NoRememberDualChoice.NO_REMEMBER.ordinal
                                        )

                                        dialogInterface.dismiss()
                                        navigateToChatroom()
                                    }
                                    .setNegativeButton(Constants.noPlease) { dialogInterface: DialogInterface, _: Int ->
                                        prefConfig.writeChatroomJoinRememberNavigate(
                                            if(dialogBinding.checkBox.isChecked) Constants.NoRememberDualChoice.REMEMBER_NO.ordinal
                                            else Constants.NoRememberDualChoice.NO_REMEMBER.ordinal
                                        )
                                        dialogInterface.dismiss()
                                    }
                                    .create()
                                    .show()
                            }
                            Constants.NoRememberDualChoice.REMEMBER_YES.ordinal -> {
                                navigateToChatroom()
                            }
                        }
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
                    if(viewModel.currentChatrooms == null) {
                        viewModel.sendIntent(ChatroomListIntent.FetchChatrooms)
                    }
                }
            }
        })
    }

    override fun observeChatroomsState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collectLatest { state ->
                when(state) {
                    is ChatroomListState.Idle -> {

                    }
                    is ChatroomListState.Loading -> {
                        // TODO: Progressbar?
                    }
                    is ChatroomListState.OnData.ChatroomsResult -> {
                        lifecycleScope.launch {
                            state.chatrooms.catch(chatroomFlowCollectExceptionHandler).collectLatest { data ->
                                chatroomListAdapter.submitData(data)
                            }
                        }
                    }
                    is ChatroomListState.OnData.JoinedChatroomsResult -> {
                        // Do nothing but don't throw exception either
                        Log.i("ChatroomListFragment", "Bruh JoinedChtroomsResult in ChatroomListFragment")
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
    }

    override fun navigateToChatroom() {
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

    override fun navigateToChatroomCreate() {
        navController.navigate(
            ChatroomListFragmentDirections.actionChatroomListFragmentToChatroomCreateNavGraph(
                activityViewModel.authUser.value!!
            ))
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }
}