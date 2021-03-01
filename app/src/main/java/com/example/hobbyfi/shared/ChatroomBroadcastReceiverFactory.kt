package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.IllegalArgumentException

// not *exactly* a factory...? but... eh???
@ExperimentalCoroutinesApi
open class ChatroomBroadcastReceiverFactory(
    protected val chatroomActivityViewModel: ChatroomActivityViewModel? = null,
    lifecycleOwner: LifecycleOwner
) : LifecycleAwareBroadcastReceiverFactory(lifecycleOwner) {
    protected val authUserIdChecker = { idGenerator: (Intent) -> Long? -> { intent: Intent ->
        idGenerator(intent) !=
                (chatroomActivityViewModel!!.authUser.value ?:
                error("Auth user in ViewModel ID must not be null in call to Create Message from BroadcastReceiver!"))
                    .id
        }
    }

    private val authChatroomOwnerChecker = { _: Intent ->
        if(chatroomActivityViewModel != null) !chatroomActivityViewModel.isAuthUserChatroomOwner.value!! else true
    }

    private val authUserIdModelChecker = authUserIdChecker {
        it.getParcelableExtra<User>(Constants.PARCELABLE_MODEL)!!.id
    }

    private val authUserIdDeleteChecker = authUserIdChecker { it.getDeletedModelIdExtra() }

    protected fun authUserIdMapChecker(idField: String) = authUserIdChecker { (it.getDestructedMapExtra()[idField]
        ?: error("User ID must not be null in call to Edit user or BroadcastReceiver callbacks which use map"))
        .toLong() }

    // FIXME: Code dup with logging messages
    override fun createActionatedReceiver(action: String): BroadcastReceiver = when(action) {
            Constants.JOIN_USER_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendUsersIntent(
                                UserListIntent.AddAUserCache(
                                    it.getParcelableExtra(Constants.PARCELABLE_MODEL)!!
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for JOIN USEEEEEEER",
                    onNoNotifyLog = "Current broadcastreceiver for joinuser should be visible to EVEN the owner. " +
                        "Something is wrong and onReceive proper action was NOT performed",
                    isNotChatroomOwnerOrShouldSee = authUserIdModelChecker
                )
            }
            Constants.LEAVE_USER_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendUsersIntent(
                                UserListIntent.DeleteAUserCache(
                                    it.getDeletedModelIdExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for LEAVE USEEEEEEER",
                    onNoNotifyLog = "Current broadcastreceiver for leaveuser should be visible to EVEN the owner. " +
                            "Something is wrong and onReceive proper action was NOT performed",
                    isNotChatroomOwnerOrShouldSee = authUserIdChecker { -1L } // always true (visible to everyone)
                )
            }
            Constants.EDIT_USER_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendUsersIntent(
                                UserListIntent.UpdateAUserCache(
                                    it.getDestructedMapExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for EDIT USEEEEEEER",
                    onNoNotifyLog = "Current broadcastreceiver for edituser should be visible to EVEN the owner. " +
                            "Something is wrong and onReceive proper action was NOT performed",
                    isNotChatroomOwnerOrShouldSee = authUserIdMapChecker(Constants.ID)
                )
            }
            Constants.EDIT_CHATROOM_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendChatroomIntent(
                                ChatroomIntent.UpdateChatroomCache(
                                    it.getDestructedMapExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for EDIT CHATROOOOOM",
                    onNoNotifyLog = "Current broadcastreceiver for editchatroom has targeted auth user owner of chatroom. " +
                        "Aborting UPDATE chatroom intent",
                    authChatroomOwnerChecker
                )
            }
            Constants.DELETE_CHATROOM_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendChatroomIntent(
                                ChatroomIntent.DeleteChatroomCache()
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE CHATROOOOOM",
                    onNoNotifyLog = "Current broadcastreceiver for deletechatroom has targeted auth user owner of chatroom. " +
                            "Aborting DELETE chatroom intent",
                    authChatroomOwnerChecker
                )
            }
            else -> throw IllegalArgumentException(Constants.invalidBroadcastAction)
        }

    companion object {
        private var instance: ChatroomBroadcastReceiverFactory? = null

        fun getInstance(viewModel: ChatroomActivityViewModel, lifecycleOwner: LifecycleOwner): ChatroomBroadcastReceiverFactory {
            synchronized(this) {
                var instance = this.instance
                if(instance == null) {
                    instance = ChatroomBroadcastReceiverFactory(viewModel, lifecycleOwner)
                }
                return instance
            }
        }
    }
}