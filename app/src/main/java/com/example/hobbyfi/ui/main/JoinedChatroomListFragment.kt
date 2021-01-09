package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// TODO: This fragment is the third one in the bottom nav
// TODO: To be implemented for displaying ONLY JOINED chatrooms (not ones the user hasn't selected yet)
// TODO: THIS IS DONE so that the user can still join/leave chatrooms dynamically
// TODO: Part of the one-to-many user chatrooms connection
// TODO: Use a shared view model and just filter data and display leave/join buttons accordingly
// TODO: UserJoinedChatrooms is pagingdata as well backed by cache
// TODO: Joined chatrooms are added once `userChatroomIds` change in UpdateUserResult UserState
@ExperimentalPagingApi
@ExperimentalCoroutinesApi
class JoinedChatroomListFragment : MainListFragment<JoinedChatroomListAdapter>() {
    private val viewModel: ChatroomListFragmentViewModel by viewModels()

    override val chatroomListAdapter: JoinedChatroomListAdapter = JoinedChatroomListAdapter({ view: View, chatroom: Chatroom ->

    }, { _: View, chatroom: Chatroom ->
        viewModel.setButtonSelectedChatroom(chatroom)
        lifecycleScope.launch {
            activityViewModel.sendIntent(
                UserIntent.UpdateUser(mapOf(
                Pair(Constants.LEAVE_CHATROOM_ID, chatroom.id.toString())
            )))
        }
    })

    // TODO: Move chatroom card visibility here
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentChatroomListBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_chatroom_list,
            container,
            false)

        with(binding) {

            observeAuthUser()

            return@onCreateView binding.root
        }
    }

    private fun setChatroomLeaveButtonVisibility(userHasChatroom: Boolean) {
        // TODO: findItemFromCurrentPagingData uses method that generates list for current paging data each time
        // TODO: This might cause performance issues later on but solves bugs related to button visibility
        // TODO: Optimise the method
        val userOwnedChatroomIds = chatroomListAdapter.extractModelListFromCurrentPagingData().filter {
            activityViewModel.authUser.value?.chatroomIds?.contains(it.id) == true
        }.map { if(it.ownerId == activityViewModel.authUser.value!!.id) it.id else null }.filterNotNull()
        chatroomListAdapter.setUserOwnedChatroomIds(userOwnedChatroomIds)
    }

    override fun observeAuthUser() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {

            }
        }
    }
}