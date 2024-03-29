package com.example.hobbyfi.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.paging.ExperimentalPagingApi
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.example.hobbyfi.work.DeviceTokenUploadWorker
import com.example.hobbyfi.utils.WorkerUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.lang.IllegalStateException
import java.util.*


@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class NotificationMessagingService : FirebaseMessagingService(), LifecycleObserver, KodeinAware {
    private var isAppInForeground: Boolean = false
    private var isAppInFocus: Boolean = true

    override val kodein: Kodein by kodein(MainApplication.applicationContext)
    private val prefConfig: PrefConfig by instance(tag = "prefConfig")
    private val localBroadcastManager: LocalBroadcastManager by instance(tag = "localBroadcastManager")

    private var fcmForegroundBaseIntentForReactivation: Intent? = null

    private val windowManager: WindowManager = MainApplication.applicationContext.getSystemService(Context.WINDOW_SERVICE)
            as WindowManager
    private val powerManager: PowerManager = MainApplication.applicationContext.getSystemService(Context.POWER_SERVICE)
            as PowerManager

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

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onForegroundResume() {
        isAppInFocus = true
        isAppInForeground = true

        if(prefConfig.readRestartedFromChatroomTaskRoot()) {
            return
        }

        val sendIntent = fcmForegroundBaseIntentForReactivation
        if(sendIntent != null) {
            Log.i("NotificationMService", "Sending FOREGROUND_REACTIVATION_ACTION after foreground resume; base intent: ${sendIntent}")
            localBroadcastManager.sendBroadcast(Intent(Constants.FOREGROUND_REACTIVATION_ACTION).apply {
                putExtras(sendIntent)
            })
            fcmForegroundBaseIntentForReactivation = null
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onForegroundPause() {
        isAppInFocus = false
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
        var isImageMessage = false

        // create message, create event, join user, leave user, delete chatroom
        when(notificationType) {
            Constants.CREATE_MESSAGE_TYPE -> {
                intent.putParcelableMessageExtra(data)
                // TODO: Handle message being url to picture and show only "new image message received!"
                if (data[Constants.USER_SENT_ID] != null) {
                    title = resources.getString(R.string.create_message_notification_title)
                    isImageMessage = data[Constants.MESSAGE]?.let { Constants.imageRegex.matches(it) } == true
                    if(!isImageMessage) {
                        body = data[Constants.MESSAGE]
                    }
                }
            }
            Constants.CREATE_EVENT_TYPE -> {
                intent.putParcelableEventExtra(data)
                title = resources.getString(R.string.create_event_notification_title)
                body = "Take a look at the '${data[Constants.NAME]}' event and try to join in!"
            }
            Constants.EDIT_CHATROOM_TYPE, Constants.EDIT_USER_TYPE,
            Constants.EDIT_MESSAGE_TYPE, Constants.EDIT_EVENT_TYPE -> {
                intent.putDestructedMapExtra(data)
            }
            Constants.DELETE_EVENT_BATCH_TYPE -> {
                intent.putExtra(Constants.EVENT_IDS, data[Constants.EVENT_IDS])
            }
            Constants.DELETE_EVENT_TYPE -> {
                intent.putDeletedModelIdExtra(data)
            }
            Constants.DELETE_CHATROOM_TYPE -> {
                intent.putDeletedModelIdExtra(data)
                title = resources.getString(R.string.delete_chatroom_notification_title)
                body = resources.getString(R.string.delete_chatroom_notification_body)
            }
            Constants.DELETE_MESSAGE_TYPE -> {
                intent.putDeletedModelUserSentIdExtra(data)
                intent.putDeletedModelIdExtra(data)
            }
            Constants.JOIN_USER_TYPE -> {
                intent.putParcelableUserExtra(data)
                title = resources.getString(R.string.join_user_notification_title)
                body = "${data[Constants.USERNAME] ?: data[Constants.NAME]} just joined one of your chatrooms!"
            }
            Constants.LEAVE_USER_TYPE -> {
                intent.putDeletedModelIdExtra(data)
                title = resources.getString(R.string.leave_user_notification_title)
                body = "${data[Constants.USERNAME] ?: data[Constants.NAME]} just left one of your chatrooms!"
            }
        }

        try {
            intent.putExtra(Constants.ROOM_IDS, Constants.jsonConverter.fromJson(data[Constants.ROOM_IDS]) as LongArray?)
        } catch(ex: Exception) {
            Log.w("NotificationMService", "COULD NOT DESERIALIZE ROOM_IDS FROM NOTIFICATION PAYLOAD?!")
        }

        val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }

        val screenOff =
            display?.state != Display.STATE_ON && !powerManager.isInteractive

        if(((!isAppInFocus && screenOff) || (!screenOff && !isAppInForeground))) {
            Log.i("NotificationMService", "App is in DOZE/onPause state, NOT onStop(). Adding to deferred broadcasts queue")
            // localBroadcastManager.sendBroadcast(intent)
            fcmForegroundBaseIntentForReactivation = intent
            if(((title == null || body == null) && !isImageMessage)) {
                return
            }
        }

        if(title != null) {
            // title/body set => hit a possible push notification
            handlePushMessageForChatroomByLifecycle(
                Intent(Constants.FOREGROUND_REACTIVATION_ACTION).apply {
                    putExtras(intent)
                },
                title,
                body
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("NotificationMService", "onNewToken triggered!!!!")

        prefConfig.writeDeviceToken(token) // save new device token and resubscribe
        prefConfig.writeCurrentDeviceTokenUploaded(false)

        try {
            val authToken = prefConfig.getAuthUserTokenRefresh()
            if(authToken != null && authToken != Constants.INVALID_TOKEN) {
                WorkerUtils.buildAndEnqueueDeviceTokenWorker<DeviceTokenUploadWorker>(
                    authToken,
                    token, applicationContext
                )
            } else throw IllegalStateException()
        } catch(ex: Exception) {
            Log.w("NotificationMService", "onNewToken user unathenticated => NOT sending to server yet!!!!")
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.i("NotificationMService", "DELETED MESSAGES?????")
    }

    private fun handlePushMessageForChatroomByLifecycle(
        intent: Intent,
        pushTitle: String,
        pushBody: String?
    ) {
        if(!isAppInForeground) {
            // handle background the same way as killed state (hopefully)
            Log.i(
                "NotificationMService",
                "App is in BACKGROUND. Sending push notification for current intent: ${intent}"
            )
            intent.setClass(this, ChatroomActivity::class.java)

            if(pushBody != null) {
                sendNormalPushNotificationForChatroom(intent, pushTitle, pushBody)
            } else {
                // image notification for message - always
                sendImagePushNotificationForChatroom(intent, pushTitle, intent.getParcelableExtra<Message>(Constants.PARCELABLE_MODEL)!!.message)
            }
        } else {
            // send broadcast
            Log.i(
                "NotificationMService",
                "App is in FOREGROUND. Sending broadcast for RESYNCHRONIZATION. Current intent: ${intent}"
            )
            localBroadcastManager.sendBroadcast(intent)
        }
    }

    private fun sendNormalPushNotificationForChatroom(
        pushIntent: Intent,
        pushTitle: String,
        pushBody: String
    ) {
        val resultPendingIntent = buildResultPendingIntent(pushIntent)
        val notification = NotificationCompat.Builder(
            this,
            resources.getString(R.string.default_notification_channel_id)
        ).apply {
                color = ContextCompat.getColor(
                    this@NotificationMessagingService,
                    R.color.colorBackground
                )
                setContentTitle(pushTitle)
                setContentText(pushBody)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setContentIntent(resultPendingIntent)
                setSmallIcon(applicationInfo.icon)
                setAutoCancel(true)

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setChannelId(resources.getString(R.string.default_notification_channel_id))
                }
        }.build()

        sendPushNotificationToAppChannel(notification, resultPendingIntent)
    }

    private fun sendImagePushNotificationForChatroom(
        pushIntent: Intent,
        pushTitle: String,
        imageURL: String
    ) {
        Glide.with(this)
            .asBitmap()
            .load(imageURL)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val resultPendingIntent = buildResultPendingIntent(pushIntent)
                    val notification = NotificationCompat.Builder(
                        this@NotificationMessagingService,
                        resources.getString(R.string.default_notification_channel_id)
                    ).apply {
                        color = ContextCompat.getColor(
                            this@NotificationMessagingService,
                            R.color.colorBackground
                        )
                        setContentTitle(pushTitle)
                        setContentText(getString(R.string.tap_to_view_image))
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        setContentIntent(resultPendingIntent)
                        setSmallIcon(applicationInfo.icon)
                        setLargeIcon(resource)
                        setStyle(NotificationCompat.BigPictureStyle()
                            .bigPicture(resource)
                            .bigLargeIcon(null))
                        setAutoCancel(true)

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            setChannelId(resources.getString(R.string.default_notification_channel_id))
                        }
                    }.build()

                    sendPushNotificationToAppChannel(notification, resultPendingIntent)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    private fun sendPushNotificationToAppChannel(notification: Notification, resultPendingIntent: PendingIntent) {
        // >= API 26
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        with(NotificationManagerCompat.from(this)) {
            notify(resultPendingIntent.hashCode(), notification) // TODO: Probably move away from hash codes. . .
        }
    }

    private fun buildResultPendingIntent(pushIntent: Intent): PendingIntent =
        TaskStackBuilder.create(this)
            .addNextIntent(pushIntent) // add intent
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)!! // get PendingIntent in its entirety; can't be null since flags are properly set
        // always start an activity with the backstack history because the chatroom can be navigated to and from
}