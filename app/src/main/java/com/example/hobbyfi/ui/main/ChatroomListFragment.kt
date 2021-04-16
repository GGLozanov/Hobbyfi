package com.example.hobbyfi.ui.main

import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.DialogNoRemindBinding
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.extractListFromCurrentPagingData
import com.example.hobbyfi.state.ChatroomListState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomListFragment : MainListFragment<ChatroomListAdapter>() {
    override val chatroomListAdapter: ChatroomListAdapter = ChatroomListAdapter(onChatroomJoinButton, onTagsViewButton)

    override fun observeChatroomEntryState() {
        activityViewModel.joinedChatroom.observe(viewLifecycleOwner, Observer { joined ->
            fun joinChatroomAndUpdate() {
                activityViewModel.updateUserWithLatestFields()
            }

            if (joined) {
                // this check is required for whenever this observer might be trigger by CreateChatroomFragment
                val joinChatroomWithDialog = {
                    joinChatroomAndUpdate()
                    // TODO: Add clear pref option
                    when(prefConfig.readChatroomJoinRememberNavigate()) {
                        Constants.NoRememberDualChoice.NO_REMEMBER.ordinal -> {
                            val dialogBinding = DialogNoRemindBinding.inflate(layoutInflater)
                            val dialog = AlertDialog.Builder(requireContext())
                                .setView(dialogBinding.root)
                                .setPositiveButton(Constants.takeMeThere) { dialogInterface: DialogInterface, _: Int ->
                                    prefConfig.writeChatroomJoinRememberNavigate(
                                        if(dialogBinding.checkBox.isChecked) Constants.NoRememberDualChoice.REMEMBER_YES.ordinal
                                        else Constants.NoRememberDualChoice.NO_REMEMBER.ordinal
                                    )

                                    dialogInterface.dismiss()
                                    navigateToChatroomPerDeepLinkExtras()
                                }
                                .setNegativeButton(Constants.noPlease) { dialogInterface: DialogInterface, _: Int ->
                                    prefConfig.writeChatroomJoinRememberNavigate(
                                        if(dialogBinding.checkBox.isChecked) Constants.NoRememberDualChoice.REMEMBER_NO.ordinal
                                        else Constants.NoRememberDualChoice.NO_REMEMBER.ordinal
                                    )

                                    dialogInterface.dismiss()
                                }.create()
                            dialog.window!!.setBackgroundDrawableResource(R.color.colorBackground)
                            dialog.show()
                        }
                        Constants.NoRememberDualChoice.REMEMBER_YES.ordinal -> {
                            navigateToChatroomPerDeepLinkExtras()
                        }
                    }
                }

                if (viewModel.buttonSelectedChatroom != null) {
                    if(activityViewModel.deepLinkExtras != null &&
                        viewModel.buttonSelectedChatroom?.id ==
                            activityViewModel.deepLinkExtras?.getDouble(Constants.CHATROOM_ID)?.toLong()) {
                        joinChatroomWithDialog()
                    } else {
                        joinChatroomWithDialog()
                    }
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

                loadStateAdapter?.setUserChatroomOwnershipIds(chatroomListAdapter.extractListFromCurrentPagingData().filter {
                    activityViewModel.authUser.value?.chatroomIds?.contains(it.id) == true
                }.mapNotNull { if (it.ownerId == activityViewModel.authUser.value!!.id) it.id else null })

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
        super.navigateToChatroom()
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