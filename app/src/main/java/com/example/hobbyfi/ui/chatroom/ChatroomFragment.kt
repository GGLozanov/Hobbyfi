package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.navArgs
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomFragment : BaseFragment(), TextFieldInputValidationOnus {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @ExperimentalCoroutinesApi
    protected val activityViewModel: ChatroomActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: ChatroomActivityArgs by (activity as ChatroomActivity).navArgs()
        AuthUserChatroomViewModelFactory(requireActivity().application, activityArgs.user, activityArgs.chatroom)
    })
}