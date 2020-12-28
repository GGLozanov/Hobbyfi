package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import androidx.lifecycle.LifecycleOwner
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.IllegalArgumentException

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
class ChatroomMessageBroadacstReceiverFactory(
    private val viewModel: ChatroomMessageListFragmentViewModel,
    chatroomActivityViewModel: ChatroomActivityViewModel,
    lifecycleOwner: LifecycleOwner
) : ChatroomBroadcastReceiverFactory(chatroomActivityViewModel, lifecycleOwner) {

    override fun createActionatedReceiver(action: String): BroadcastReceiver = when(action) {
            Constants.CREATE_MESSAGE_TYPE -> {
                // used by both timeline and normal messages
                createReceiver(
                    action,

                )
            }
            Constants.EDIT_MESSAGE_TYPE -> {
                createReceiver(
                    action,

                )
            }
            Constants.DELETE_MESSAGE_TYPE -> {
                // TODO: Handle owner delete message and not show action; otherwise show.
                //  Or simply ignore by peeking in messages list in  and seeing if it's already deleted
                createReceiver(
                    action,
                    isNotUserChatroomOwnerOrShouldSee = { true }
                )
            }
            else -> throw IllegalArgumentException(Constants.invalidBroadcastAction)
        }
    }
}