package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.models.User
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.IllegalArgumentException

// not *exactly* a factory...? but... eh???
@ExperimentalCoroutinesApi
open class ChatroomBroadcastReceiverFactory(
    private val chatroomActivityViewModel: ChatroomActivityViewModel? = null,
    protected val lifecycleOwner: LifecycleOwner
) {

    protected val authUserIdChecker = { idGenerator: (Intent) -> Long? -> { intent: Intent ->
        idGenerator(intent) !=
                (chatroomActivityViewModel!!.authUser.value ?:
                error("Auth user in ViewModel ID must not be null in call to Create Message from BroadcastReceiver!"))
                    .id
        }
    }

    protected fun<T: Model> generateAuthUserIdModelChecker(): (Intent) -> Boolean =
        authUserIdChecker {
            it.getParcelableExtra<T>(Constants.PARCELABLE_MODEL)!!.id
        }

    private val authUserIdModelChecker = generateAuthUserIdModelChecker<User>()

    protected val authUserIdDeleteChecker = authUserIdChecker { it.getDeletedModelIdExtra() }

    protected val authUserIdMapChecker = authUserIdChecker { (it.getDestructedMapExtra()[Constants.ID]
        ?: error("User ID must not be null in call to Edit user or BroadcastReceiver callbacks which use map"))
        .toLong() }

    fun createReceiver(intentAction: String, onCorrectAction: (intent: Intent) -> Unit, onReceiveLog: String? = null,
                       onNoNotifyLog: String? = null, isNotChatroomOwnerOrShouldSee: ((intent: Intent) -> Boolean) = {
            if(chatroomActivityViewModel != null)
                !chatroomActivityViewModel.isAuthUserChatroomOwner.value!!
            else true }): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == intentAction) {
                // people do checks here ^; idk why given the intent filter
                if (onReceiveLog != null) {
                    Log.i("ChatroomActivity", onReceiveLog)
                }

                // if nothing passed => even the chatroom owner will be notified!
                if(isNotChatroomOwnerOrShouldSee(intent)) {
                    onCorrectAction(intent)
                } else {
                    if (onNoNotifyLog != null) {
                        Log.i("ChatroomActivity", onNoNotifyLog)
                    }
                }
            }
        }
    }

    // FIXME: Code dup with logging messages
    open fun createActionatedReceiver(action: String): BroadcastReceiver = when(action) {
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
                    isNotChatroomOwnerOrShouldSee = authUserIdDeleteChecker
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
                    isNotChatroomOwnerOrShouldSee = authUserIdMapChecker
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
                        "Aborting UPDATE chatroom intent"
                )
            }
            Constants.DELETE_CHATROOM_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendChatroomIntent(
                                ChatroomIntent.DeleteChatroomCache
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE CHATROOOOOM",
                    onNoNotifyLog = "Current broadcastreceiver for deletechatroom has targeted auth user owner of chatroom. " +
                            "Aborting DELETE chatroom intent"
                )
            }
            Constants.CREATE_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendEventIntent(
                                EventIntent.CreateEventCache(
                                    it.getParcelableExtra(Constants.PARCELABLE_MODEL)!!
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE CHATROOOOOM",
                    onNoNotifyLog = "Current broadcastreceiver for deletechatroom has targeted auth user owner of chatroom. " +
                            "Aborting DELETE chatroom intent"
                )
            }
            Constants.EDIT_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendEventIntent(
                                EventIntent.UpdateEventCache(
                                    it.getDestructedMapExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE CHATROOOOOM",
                    onNoNotifyLog = "Current broadcastreceiver for deletechatroom has targeted auth user owner of chatroom. " +
                            "Aborting DELETE chatroom intent"
                )
            }
            Constants.DELETE_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenStarted {
                            chatroomActivityViewModel!!.sendEventIntent(
                                EventIntent.DeleteEventCache
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE EVEEEENT",
                    onNoNotifyLog = "Current broadcastreceiver for deleteevent has targeted auth user owner of chatroom. " +
                            "Aborting DELETE event intent"
                )
            }
            else -> throw IllegalArgumentException(Constants.invalidBroadcastAction)
        }

    companion object {
        private var instance: ChatroomBroadcastReceiverFactory? = null

        fun getInstance(viewModel: ChatroomActivityViewModel? = null, lifecycleOwner: LifecycleOwner): ChatroomBroadcastReceiverFactory {
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