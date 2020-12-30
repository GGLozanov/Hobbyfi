package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.User
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

    private val authUserIdMessageChecker = generateAuthUserIdModelChecker<Message>()

    override fun createActionatedReceiver(action: String): BroadcastReceiver = when(action) {
            Constants.CREATE_MESSAGE_TYPE -> {
                // CREATE used by both timeline and normal messages
                // TODO: Handle auth user created/edited message (pass in intent to isNotChatroomOwnerOrShouldSee())
                // so that message sent user id can be acccessed and checked
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            viewModel.messageStateIntent.sendIntent(
                                MessageIntent.CreateMessageCache(
                                    it.getParcelableExtra(Constants.PARCELABLE_MODEL)!!
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for CREATE MESSAGE",
                    onNoNotifyLog = "Current broadcastreceiver for create message has targeted auth user AS owner of message. " +
                            "Aborting CREATE message intent",
                    isNotChatroomOwnerOrShouldSee = authUserIdMessageChecker
                )
            }
            Constants.EDIT_MESSAGE_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            viewModel.messageStateIntent.sendIntent(
                                MessageIntent.UpdateMessageCache(
                                    it.getDestructedMapExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for UPDATE MESSAGE",
                    onNoNotifyLog = "Current broadcastreceiver for update message has targeted auth user AS owner of message. " +
                            "Aborting UPDATE message intent",
                    isNotChatroomOwnerOrShouldSee = authUserIdMapChecker
                )
            }
            Constants.DELETE_MESSAGE_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            viewModel.messageStateIntent.sendIntent(
                                MessageIntent.DeleteMessageCache(
                                    it.getDeletedModelIdExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE MESSAGE",
                    onNoNotifyLog = "Current broadcastreceiver for DELETE message has targeted auth user AS owner of message. " +
                            "Aborting DELETE message intent",
                    isNotChatroomOwnerOrShouldSee = authUserIdDeleteChecker
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