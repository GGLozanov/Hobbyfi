package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.navArgs
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthChatroomViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomFragment : BaseFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @ExperimentalCoroutinesApi
    protected val activityViewModel: ChatroomActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: ChatroomActivityArgs by (activity as ChatroomActivity).navArgs()
        AuthChatroomViewModelFactory(requireActivity().application, activityArgs.user, activityArgs.chatroom)
    })
}