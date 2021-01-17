package com.example.hobbyfi.shared

import android.content.BroadcastReceiver
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
class EventBroadcastReceiverFactory private constructor(
    private val eventMapsActivityViewModel: EventMapsActivityViewModel? = null,
    private val chatroomActivityViewModel: ChatroomActivityViewModel? = null,
    lifecycleOwner: LifecycleOwner,
) : LifecycleAwareBroadcastReceiverFactory(lifecycleOwner) {

    constructor(eventMapsActivityViewModel: EventMapsActivityViewModel,
                lifecycleOwner: LifecycleOwner) : this(eventMapsActivityViewModel, null, lifecycleOwner)

    constructor(chatroomActivityViewModel: ChatroomActivityViewModel,
                lifecycleOwner: LifecycleOwner) : this(null, chatroomActivityViewModel, lifecycleOwner)

    private fun eventIdChecker(eventId: Long? = null): Boolean =
        if(chatroomActivityViewModel == null) eventId == eventMapsActivityViewModel!!.event.value?.id else
            !chatroomActivityViewModel.isAuthUserChatroomOwner.value!!

    private fun eventIdBatchChecker(eventIds: List<Long>? = null): Boolean =
        if(chatroomActivityViewModel == null) eventIds!!.contains(eventMapsActivityViewModel!!.event.value?.id) else
            !chatroomActivityViewModel.isAuthUserChatroomOwner.value!!

    private val eventIdEditChecker: (Intent) -> Boolean = { intent: Intent -> eventIdChecker((
            intent.getDestructedMapExtra()[Constants.ID] ?: error("Event ID must not be null in Event Edit Id Checker!")).toLong()) }
    private val eventIdDeleteChecker: (Intent) -> Boolean = { intent: Intent -> eventIdChecker(
        intent.getDeletedModelIdExtra()) }
    private val eventIdBatchDeleteChecker: (Intent) -> Boolean = { intent: Intent -> eventIdBatchChecker(
        Constants.tagJsonConverter.fromJson(intent.getDestructedMapExtra()[Constants.EVENT_IDS])) }
    private val eventIdCreateChecker: (Intent) -> Boolean = {
        chatroomActivityViewModel?.isAuthUserChatroomOwner?.value == false }

    override fun createActionatedReceiver(action: String): BroadcastReceiver =
        // always see these messages because owner CANNOT edit/delete chatroom while in Maps activity
        // (which is where the receivers are registered and active)
        when(action) {
            Constants.CREATE_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = { intent ->
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            chatroomActivityViewModel!!.sendEventsIntent(
                                EventListIntent.AddAnEventCache(
                                    intent.getParcelableExtra(Constants.PARCELABLE_MODEL)!!
                                )
                            )
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE CHATROOOOOM",
                    isNotChatroomOwnerOrShouldSee = eventIdCreateChecker
                )
            }
            Constants.EDIT_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = { intent ->
                        val updateFields = intent.getDestructedMapExtra()
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            executeSuspendOnAvailableViewModel({
                                it.sendEventsIntent(
                                    EventListIntent.UpdateAnEventCache(
                                        updateFields
                                    )
                                )
                            }, {
                                it.sendEventsIntent(
                                    EventListIntent.UpdateAnEventCache(
                                        updateFields
                                    )
                                )
                            })
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for EDIT EVENT",
                    isNotChatroomOwnerOrShouldSee = eventIdEditChecker
                )
            }
            Constants.DELETE_EVENT_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = { intent ->
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            executeSuspendOnAvailableViewModel({
                                it.sendEventsIntent(
                                    EventListIntent.DeleteAnEventCache(
                                        intent.getDeletedModelIdExtra()
                                    )
                                )
                            }, {
                                it.sendEventsIntent(
                                    EventListIntent.DeleteAnEventCache(
                                        intent.getDeletedModelIdExtra()
                                    )
                                )
                            })
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE EVEEEENT",
                    isNotChatroomOwnerOrShouldSee = eventIdDeleteChecker
                )
            }
            Constants.DELETE_EVENT_BATCH_TYPE -> {
                createReceiver(
                    action,
                    onCorrectAction = { intent ->
                        lifecycleOwner.lifecycleScope.launchWhenCreated {
                            executeSuspendOnAvailableViewModel({
                                it.sendEventsIntent(
                                    EventListIntent.DeleteEventsCache(
                                        intent.getEventIdsExtra()
                                    )
                                )
                            }, {
                                it.sendEventsIntent(
                                    EventListIntent.DeleteEventsCache(
                                        intent.getEventIdsExtra()
                                    )
                                )
                            })
                        }
                    },
                    onReceiveLog = "Got me a broadcast receievrino for DELETE EVEEEENT BATCH",
                    isNotChatroomOwnerOrShouldSee = eventIdBatchDeleteChecker
                )
            }
            else -> throw IllegalArgumentException(Constants.invalidBroadcastAction)
        }

    private suspend fun executeSuspendOnAvailableViewModel(
            onChatroomActivityViewModel: suspend (ChatroomActivityViewModel) -> Unit,
            onEventMapsActivityViewModel: suspend (EventMapsActivityViewModel) -> Unit
    ) {
        when {
            eventMapsActivityViewModel != null -> {
                onEventMapsActivityViewModel(eventMapsActivityViewModel)
            }
            chatroomActivityViewModel != null -> {
                onChatroomActivityViewModel(chatroomActivityViewModel)
            }
            else -> {
                throw IllegalStateException("Both EventBroadcastReceiverFactory ViewModels cannot be null!")
            }
        }
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

        fun getInstance(viewModel: ChatroomActivityViewModel, lifecycleOwner: LifecycleOwner): EventBroadcastReceiverFactory {
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