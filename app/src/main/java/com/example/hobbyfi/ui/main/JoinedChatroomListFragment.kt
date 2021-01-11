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
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.chatroom.JoinedChatroomListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomListBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.extractModelListFromCurrentPagingData
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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

    override suspend fun observeChatroomsState() {
        viewModel.mainState.collect {
            // TODO: On error for fetch, check if ChatroomListFragment has chatrooms and put them in by filtering
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
            }
        })
    }

    private fun leaveChatroom() {
        viewModel.setButtonSelectedChatroom(null)
        viewModel.setCurrentChatrooms(null)
    }
}