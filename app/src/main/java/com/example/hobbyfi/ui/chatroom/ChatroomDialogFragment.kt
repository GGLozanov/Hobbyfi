package com.example.hobbyfi.ui.chatroom

import androidx.fragment.app.activityViewModels
import androidx.navigation.navArgs
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomDialogFragment : BaseDialogFragment(), TextFieldInputValidationOnus {
    @ExperimentalCoroutinesApi
    protected val activityViewModel: ChatroomActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: ChatroomActivityArgs by (activity as ChatroomActivity).navArgs()
        AuthUserChatroomViewModelFactory(requireActivity().application, activityArgs.user, activityArgs.chatroom)
    })
}