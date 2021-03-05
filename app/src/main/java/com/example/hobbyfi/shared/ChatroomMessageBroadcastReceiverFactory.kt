package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.adapters.message.ChatroomMessageListAdapter
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.IllegalArgumentException

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
class ChatroomMessageBroadcastReceiverFactory(
    private val viewModel: ChatroomMessageListFragmentViewModel,
    private val messagesAdapter: ChatroomMessageListAdapter,
    chatroomActivityViewModel: ChatroomActivityViewModel? = null,
    lifecycleOwner: LifecycleOwner
) : ChatroomBroadcastReceiverFactory(chatroomActivityViewModel, lifecycleOwner) {

    private val authUserIdMessageChecker = authUserIdChecker {
        it.getParcelableExtra<Message>(Constants.PARCELABLE_MODEL)!!.userSentId
    }

    override fun createActionatedReceiver(action: String): BroadcastReceiver = when(action) {
            Constants.CREATE_MESSAGE_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        val message = it.getParcelableExtra(Constants.PARCELABLE_MODEL) as Message?
                        if(chatroomActivityViewModel?.authUser?.value?.chatroomIds?.contains(message?.chatroomSentId) == true) {
                            // assert user still in chatroom (kick race condition)
                            lifecycleOwner.lifecycleScope.launchWhenCreated {
                                viewModel.messageStateIntent.sendIntent(
                                    MessageIntent.CreateMessageCache(
                                        message!!
                                    )
                                )
                            }
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
                        if(messagesAdapter.findItemFromCurrentPagingData { msg -> msg is UIMessage.MessageItem && msg.message.id ==
                                    it.getDestructedMapExtra()[Constants.ID]?.toLong() } != null &&
                                chatroomActivityViewModel?.authUser?.value?.chatroomIds?.contains(
                                    it.getDestructedMapExtra()[Constants.CHATROOM_SENT_ID]?.toLong()) == true) {
                            // only update if item is currently visible in pages
                            lifecycleOwner.lifecycleScope.launchWhenCreated {
                                viewModel.messageStateIntent.sendIntent(
                                    MessageIntent.UpdateMessageCache(
                                        it.getDestructedMapExtra()
                                    )
                                )
                            }
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for UPDATE MESSAGE",
                    onNoNotifyLog = "Current broadcastreceiver for update message has targeted auth user AS owner of message. " +
                            "Aborting UPDATE message intent",
                    isNotChatroomOwnerOrShouldSee = authUserIdMapChecker(Constants.USER_SENT_ID)
                )
            }
            Constants.DELETE_MESSAGE_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        if(messagesAdapter.findItemFromCurrentPagingData {
                                    msg -> msg is UIMessage.MessageItem && msg.message.id == it.getDeletedModelIdExtra() } != null &&
                            chatroomActivityViewModel?.authUser?.value?.chatroomIds?.contains(
                                chatroomActivityViewModel.authChatroom.value?.id
                            ) == true) {
                            // only delete if message currently visible
                            lifecycleOwner.lifecycleScope.launchWhenCreated {
                                viewModel.messageStateIntent.sendIntent(
                                    MessageIntent.DeleteMessageCache(
                                        it.getDeletedModelIdExtra()
                                    )
                                )
                            }
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE MESSAGE",
                    onNoNotifyLog = "Current broadcastreceiver for DELETE message has targeted auth user AS owner of message. " +
                            "Aborting DELETE message intent",
                    isNotChatroomOwnerOrShouldSee = authUserIdChecker {
                        return@authUserIdChecker it.extras!!.getLong(Constants.DELETED_MODEL_USER_SENT_ID)
                    }
                )
            }
            else -> throw IllegalArgumentException(Constants.invalidBroadcastAction)
        }

    companion object {
        private var instance: ChatroomMessageBroadcastReceiverFactory? = null

        fun getInstance(viewModel: ChatroomMessageListFragmentViewModel,
                        adapter: ChatroomMessageListAdapter,
                        chatroomActivityViewModel: ChatroomActivityViewModel? = null,
                        lifecycleOwner: LifecycleOwner): ChatroomMessageBroadcastReceiverFactory {
            synchronized(this) {
                var instance = this.instance
                if(instance == null) {
                    instance = ChatroomMessageBroadcastReceiverFactory(viewModel, adapter,
                        chatroomActivityViewModel, lifecycleOwner)
                }
                return instance
            }
        }
    }
}