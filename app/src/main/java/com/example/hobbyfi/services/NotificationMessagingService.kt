package com.example.hobbyfi.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class NotificationMessagingService : FirebaseMessagingService(), LifecycleObserver, KodeinAware {
    private var isAppInForeground: Boolean = false

    override val kodein: Kodein by kodein(MainApplication.applicationContext)
    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

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
        Log.i("NotificationMService", "FCM MESSAGE DATA BEYBEEEE: ${message.data}")
        // message has `type` key in data payload that specifies the message type (ex: `EDIT_CHATROOM`, `DELETE_CHATROOM`, etc.)
        val data = message.data // nullable types like 'description', etc. are sent as empty string or "0" and handled by the models
        val notificationType = data[Constants.TYPE]
        val intent = Intent(notificationType) // action to trigger registered receivers for given notification types

        var title: String? = null
        var body: String? = null

        // TODO: Add support for inputting and parsing one-to-many connections for models
        when(notificationType) {
            Constants.CREATE_MESSAGE_TYPE -> {
                intent.putParcelableMessageExtra(data)
                // TODO: Add message content to notification body while trimming it
                // TODO: Handle message being url to picture and show only "new image message received!"
                if(data[Constants.USER_SENT_ID] != null) {
                    title = resources.getString(R.string.create_message_notification_title)
                    body = data[Constants.MESSAGE]
                }
            }
            Constants.CREATE_EVENT_TYPE ->  {
                intent.putParcelableEventExtra(data)
                title = resources.getString(R.string.create_event_notification_title)
                body = "Take a look at '${data[Constants.NAME]}' and try to join in!"
            }
            Constants.EDIT_CHATROOM_TYPE, Constants.EDIT_USER_TYPE,
            Constants.EDIT_MESSAGE_TYPE, Constants.EDIT_EVENT_TYPE, Constants.DELETE_EVENT_BATCH_TYPE -> {
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
                intent.putParcelableUserExtra(data)
                title = resources.getString(R.string.join_user_notification_title)
                body = "${data[Constants.USERNAME]} just joined the chatroom!"
            }
            Constants.LEAVE_USER_TYPE -> {
                intent.putDeletedModelIdExtra(data)
                title = resources.getString(R.string.leave_user_notification_title)
                body = "${data[Constants.USERNAME]} just left the chatroom!"
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
            Log.i("NotificationMService", "Normal notification detected. Simply sending broadcast!")
            sendBroadcast(intent)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val oldToken = prefConfig.readDeviceToken() // old device token for unsubscription
        // TODO: ping IID (instance ID) endpoint for topic unsubscription?

        prefConfig.writeDeviceToken(token) // save new device token and resubscribe


        // dunno what to do here for now; not using specifics tokens for now so /shrug
        // redo subscriptions to topics here somehow...
        // ...send broadcast?
        // or schedule tasks with WorkManager to reissue subscriptions there with the db
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.i("NotificationMService", "DELETED MESSAGES?????")
    }

    private fun handlePushMessageForChatroomByLifecycle(intent: Intent, pushTitle: String, pushBody: String) {
        if(isAppInForeground) {
            // send broadcast
            Log.i("NotificationMService", "App is in FOREGROUND. Sending broadcast for current intent: ${intent}")
            sendBroadcast(intent)
        } else {
            // handle background the same way as killed state (hopefully)
            Log.i("NotificationMService", "App is in BACKGROUND. Sending push notification for current intent: ${intent}")
            intent.setClass(this, ChatroomActivity::class.java)
            sendPushNotificationForChatroom(intent, pushTitle, pushBody)
        }
    }

    private fun sendPushNotificationForChatroom(pushIntent: Intent, pushTitle: String, pushBody: String) {
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntent(pushIntent) // add intent
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT) // get PendingIntent in its entirety
        } // always start an activity with the backstack history because the chatroom can be navigated to and from

        val notification = NotificationCompat.Builder(
            this,
            resources.getString(R.string.default_notification_channel_id)).apply {
                color = ContextCompat.getColor(this@NotificationMessagingService, R.color.colorBackground)
                setContentTitle(pushTitle)
                setContentText(pushBody)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setContentIntent(resultPendingIntent)
                setSmallIcon(applicationInfo.icon)
                setAutoCancel(true)
        }.build()

        createNotificationChannel()

        with(NotificationManagerCompat.from(this)) {
            notify(resultPendingIntent.hashCode(), notification) // TODO: Probably move away from hash codes. . .
        }
    }

    private fun createNotificationChannel() {
        // > API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = resources.getString(R.string.default_notification_channel_id)
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                channelId,
                name,
                importance
            ).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}