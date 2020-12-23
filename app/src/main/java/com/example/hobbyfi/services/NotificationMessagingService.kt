package com.example.hobbyfi.services

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class NotificationMessagingService : FirebaseMessagingService(), LifecycleObserver {
    private var isAppInForeground: Boolean = false

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForegroundStart() {
        isAppInForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onForegroundStop() {
        isAppInForeground = false
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i("NotificationMService", "WOOOOOOOOOO, FCM MESSAGE RECEIVED BEYBEEEE: ${message}")
        // message has `type` key in data payload that specifies the message type (ex: `EDIT_CHATROOM`, `DELETE_CHATROOM`, etc.)
        val data = message.data // nullable types like 'description', etc. are sent as empty string or "0" and handled by the models
        val notificationType = data[Constants.TYPE]
        val intent = Intent(notificationType) // action to trigger registered receivers for given notification types

        var title: String? = null
        var body: String? = null
        when(notificationType) {
            Constants.CREATE_MESSAGE_TYPE -> {
                intent.putParcelableMessageExtra(data)
                // TODO: Add message content to notification body while trimming it
                // TODO: Handle message being url to picture and show only "new image message received!"
                title = resources.getString(R.string.create_message_notification_title)
                body = "YOUR MESSAGE SHOULD BE HERE BUT APP IS STILL WIP"
            }
            Constants.CREATE_EVENT_TYPE ->  {
                // TODO: Push Notification
                intent.putParcelableEventExtra(data)
                title = resources.getString(R.string.create_event_notification_title)
                // TODO: Finish
                body = "Take a look at <EVENT_NAME> and try to join in!"
            }
            Constants.EDIT_CHATROOM_TYPE, Constants.EDIT_USER_TYPE,
            Constants.EDIT_MESSAGE_TYPE, Constants.EDIT_EVENT_TYPE -> {
                intent.putDestructedMapExtra(data)
            }
            Constants.DELETE_CHATROOM_TYPE, Constants.DELETE_EVENT_TYPE -> {
                intent.putDeletedModelIdExtra(data)
                title = resources.getString(R.string.delete_chatroom_notification_title)
                body = resources.getString(R.string.delete_chatroom_notification_body)
            }
            Constants.DELETE_MESSAGE_TYPE -> {
                intent.putDeletedModelIdExtra(data)
            }
            Constants.JOIN_USER_TYPE -> {
                // TODO: Push Notification
                intent.putParcelableUserExtra(data)
                title = resources.getString(R.string.join_user_notification_title)
                body = "<USERNAME> just joined the chatroom!"
            }
            Constants.LEAVE_USER_TYPE -> {
                // TODO: Push Notification
                intent.putDeletedModelIdExtra(data)
                title = resources.getString(R.string.leave_user_notification_title)
                body = "<USERNAME> just left the chatroom!"
            }
        }

        if(title != null && body != null) {
            // title & body set => hit a possible push notification
            handlePushMessageForChatroomByLifecycle(
                intent,
                title,
                body
            )
        } else {
            // not push notification
            sendBroadcast(intent)
        }


        // try each model's `canBeModelledFromMap` method on RemoteMessage map payload to see if it can be directly mapped
        // if it can then model it directly (first check if `type` key is prefixed with `CREATE_`)
        // otherwise if it can't be mapped and not all the fields are present AND the notification is of type EDIT (prefixed with "EDIT_")
        // send the data payload (even the `type` key) with the proper action for the broadcastreceiver intent garnered from the `type` key

        // TODO: Might not need all of these checks from models and missing fields; just check the notification `type` key and act upon that no matter what

        // update chatroom message => has ONLY updated fields in notification
        // if update chatroom message is last event id change update the cache and don't do anything special; else => show toast
        // event create notification should be triggered after that
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // dunno what to do here for now; not using specifics tokens for now so /shrug
        // redo subscriptions to topics here somehow...
        // ...send broadcast?
        // or schedule tasks with WorkManager to reissue subscriptions there with the db
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    private fun handlePushMessageForChatroomByLifecycle(intent: Intent, pushTitle: String, pushBody: String) {
        if(isAppInForeground) {
            // send broadcast
            sendBroadcast(intent)
        } else {
            // handle background the same way as killed state (hopefully)
            intent.setClass(this, ChatroomActivity::class.java)
            sendPushNotificationForChatroom(intent, pushTitle, pushBody)
        }
    }

    private fun sendPushNotificationForChatroom(pushIntent: Intent, pushTitle: String, pushBody: String) {
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(pushIntent) // add intent to inflate back stack
            getPendingIntent(pushIntent.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT) // get PendingIntent in its entirety
        } // always start an activity with the backstack history because the chatroom can be navigated to and from

        val notification = NotificationCompat.Builder(
            this,
            resources.getString(R.string.default_notification_channel_id)).apply {
                color = ContextCompat.getColor(this@NotificationMessagingService, R.color.colorBackground)
                setContentTitle(pushTitle)
                setContentText(pushBody)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setContentIntent(resultPendingIntent)
                setAutoCancel(true)
        }.build()

        with(NotificationManagerCompat.from(this)) {
            notify(resultPendingIntent.hashCode(), notification) // TODO: Probably move away from hash codes. . .
        }
    }
}