package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.IllegalArgumentException

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
class ChatroomMessageBroadacastReceiverFactory(
    private val viewModel: ChatroomMessageListFragmentViewModel,
    chatroomActivityViewModel: ChatroomActivityViewModel? = null,
    lifecycleOwner: LifecycleOwner
) : ChatroomBroadcastReceiverFactory(chatroomActivityViewModel, lifecycleOwner) {

    override fun createActionatedReceiver(action: String): BroadcastReceiver = when(action) {
            Constants.CREATE_MESSAGE_TYPE, Constants.EDIT_MESSAGE_TYPE -> {
                // CREATE used by both timeline and normal messages
                // TODO: Handle auth user created/edited message (pass in intent to isNotChatroomOwnerOrShouldSee())
                // so that message sent user id can be acccessed and checked
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            viewModel.messageStateIntent.sendIntent(
                                MessageIntent.UpdateMessageCache(
                                    it.getParcelableExtra(Constants.PARCELABLE_MODEL)!!
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for CREATE/EDIT MESSAGE",
                    onNoNotifyLog = "Current broadcastreceiver for create/edit message has targeted auth user AS owner of message. " +
                            "Aborting CREATE/UPDATE message intent"
                )
            }
            Constants.DELETE_MESSAGE_TYPE -> {
                // TODO: Handle auth user delete message and not show action; otherwise show.
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            viewModel.messageStateIntent.sendIntent(
                                MessageIntent.DeleteMessageCache(
                                    it.getParcelableExtra(Constants.DELETED_MODEL_ID)!!
                                )
                            )
                        }
                    },
                    onReceiveLog = null,
                    onNoNotifyLog = "Current broadcastreceiver for DELETE message has targeted auth user AS owner of message. " +
                            "Aborting DELETE message intent"
                )
            }
            else -> throw IllegalArgumentException(Constants.invalidBroadcastAction)
        }

    companion object {
        private var instance: ChatroomMessageBroadacastReceiverFactory? = null

        fun getInstance(viewModel: ChatroomMessageListFragmentViewModel,
                        chatroomActivityViewModel: ChatroomActivityViewModel? = null,
                        lifecycleOwner: LifecycleOwner): ChatroomMessageBroadacastReceiverFactory {
            synchronized(this) {
                var instance = this.instance
                if(instance == null) {
                    instance = ChatroomMessageBroadacastReceiverFactory(viewModel, chatroomActivityViewModel, lifecycleOwner)
                }
                return instance
            }
        }
    }
}