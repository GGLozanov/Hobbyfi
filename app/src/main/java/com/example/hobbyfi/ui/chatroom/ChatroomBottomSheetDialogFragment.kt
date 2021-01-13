package com.example.hobbyfi.ui.chatroom

import androidx.fragment.app.activityViewModels
import androidx.navigation.navArgs
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomBottomSheetDialogFragment : BottomSheetDialogFragment() {
    @ExperimentalCoroutinesApi
    protected val activityViewModel: ChatroomActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: ChatroomActivityArgs by (requireActivity() as ChatroomActivity).navArgs()
        AuthUserChatroomViewModelFactory(requireActivity().application, activityArgs.user, activityArgs.chatroom)
    })
}