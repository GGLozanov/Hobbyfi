package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import java.lang.IllegalArgumentException

class EventBroadcastReceiverFactory(
    private val eventMapsActivityViewModel: EventMapsActivityViewModel,
    lifecycleOwner: LifecycleOwner
) : LifecycleAwareBroadcastReceiverFactory(lifecycleOwner) {

    override fun createActionatedReceiver(action: String): BroadcastReceiver =
        // always see these messages because owner CANNOT edit/delete chatroom while in Maps activity
        // (which is where the receivers are registered and active)
        when(action) {
            Constants.EDIT_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            eventMapsActivityViewModel.sendEventIntent(
                                EventListIntent.UpdateAnEventCache(
                                    it.getDestructedMapExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE CHATROOOOOM",
                    isNotChatroomOwnerOrShouldSee = { true }
                )
            }
            Constants.DELETE_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            eventMapsActivityViewModel.sendEventIntent(
                                EventListIntent.DeleteAnEventCache(it.getDeletedModelIdExtra())
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE EVEEEENT",
                    isNotChatroomOwnerOrShouldSee = { true }
                )
            }
            Constants.DELETE_EVENT_BATCH_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = {
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            eventMapsActivityViewModel.sendEventIntent(
                                EventListIntent.DeleteEventsCache(
                                    it.getEventIdsExtra()
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE EVEEEENT BATCH",
                    isNotChatroomOwnerOrShouldSee = { true }
                )
            }
            else -> throw IllegalArgumentException(Constants.invalidBroadcastAction)
        }

    companion object {
        private var instance: EventBroadcastReceiverFactory? = null

        fun getInstance(viewModel: EventMapsActivityViewModel, lifecycleOwner: LifecycleOwner): EventBroadcastReceiverFactory {
            synchronized(this) {
                var instance = this.instance
                if(instance == null) {
                    instance = EventBroadcastReceiverFactory(viewModel, lifecycleOwner)
                }
                return instance
            }
        }
    }
}