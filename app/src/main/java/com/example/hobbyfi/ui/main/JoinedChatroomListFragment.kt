package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.adapters.chatroom.JoinedChatroomListAdapter
import com.example.hobbyfi.intents.ChatroomListIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.extractModelListFromCurrentPagingData
import com.example.hobbyfi.state.ChatroomListState
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
class JoinedChatroomListFragment : MainListFragment<JoinedChatroomListAdapter>() {
    override val chatroomListAdapter: JoinedChatroomListAdapter = JoinedChatroomListAdapter(onChatroomJoinButton, { _: View, chatroom: Chatroom ->
        viewModel.setButtonSelectedChatroom(chatroom)
        lifecycleScope.launch {
            activityViewModel.sendIntent(
                UserIntent.UpdateUser(mapOf(
                    Pair(Constants.LEAVE_CHATROOM_ID, chatroom.id.toString())
            )))
        }
    })

    private fun setChatroomLeaveButtonVisibility() {
        // TODO: findItemFromCurrentPagingData uses method that generates list for current paging data each time
        // TODO: This might cause performance issues later on but solves bugs related to button visibility
        // TODO: Optimise the method
        val userOwnedChatroomIds =
            chatroomListAdapter.extractModelListFromCurrentPagingData().filter {
                activityViewModel.authUser.value?.chatroomIds?.contains(it.id) == true
            }.mapNotNull { if (it.ownerId == activityViewModel.authUser.value!!.id) it.id else null }
        chatroomListAdapter.setUserOwnedChatroomIds(userOwnedChatroomIds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun observeChatroomsState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                // TODO: On error for fetch, check if ChatroomListFragment has chatrooms and put them in by filtering
                when(it) {
                    is ChatroomListState.Idle -> {

                    }
                    is ChatroomListState.Loading -> {

                    }
                    is ChatroomListState.OnData.ChatroomsResult -> {
                        // Do nothing but don't throw exception either
                        Log.i("JChatroomListFragment", "Bruh ChatroomsResult in JoinedChatroomListFragment")
                    }
                    is ChatroomListState.OnData.JoinedChatroomsResult -> {
                        Log.i("JoinedCListFragment", "Received chatrooms user's already apart of!")
                        it.joinedChatrooms.catch(chatroomFlowCollectExceptionHandler).collectLatest { chatrooms ->
                            setChatroomLeaveButtonVisibility()

                            chatroomListAdapter.submitData(chatrooms)
                        }
                    }
                    is ChatroomListState.Error -> { // FIXME: Code dup here with other error handlings
                        if(it.shouldReauth) {
                            (requireActivity() as MainActivity).logout()
                        }

                        Toast.makeText(requireContext(), it.error, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }

    override fun observeChatroomEntryState() {
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

    override fun observeAuthUser() {
        activityViewModel.authUser.observe(viewLifecycleOwner, Observer { user ->
            if(user != null) {

                setChatroomLeaveButtonVisibility()

                Log.i("JoinedCListFragment", "currentJoinedChatrooms: ${viewModel.currentJoinedChatrooms}")

                // TODO: Add condition if chatroom ids have changed
                if(viewModel.currentJoinedChatrooms == null && user.chatroomIds != null) {
                    lifecycleScope.launch {
                        viewModel.sendIntent(ChatroomListIntent.FetchJoinedChatrooms(user.chatroomIds!!))
                    }
                }
            }
        })
    }

    private fun leaveChatroom() {
        viewModel.setButtonSelectedChatroom(null)
        // viewModel.setCurrentChatrooms(null)
    }
}