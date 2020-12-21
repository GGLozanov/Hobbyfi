package com.example.hobbyfi.services

import android.content.Intent
import android.os.Build
import android.util.ArrayMap
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

// TODO: Architecture built only on data notifications with push notifications created locally here
class NotificationMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i("NotificationMService", "WOOOOOOOOOO, FCM MESSAGE RECEIVED BEYBEEEE: ${message}")
        // message has `type` key in data payload that specifies the message type (ex: `EDIT_CHATROOM`, `DELETE_CHATROOM`, etc.)

        val notificationType = message.data[Constants.TYPE]
        val intent = Intent(notificationType) // action to trigger registered receivers for given notification types

        when(notificationType) {
            Constants.CREATE_MESSAGE_TYPE -> {
                // TODO: Push Notification
            }
            Constants.CREATE_EVENT_TYPE ->  {
                // TODO: Push Notification
            }
            Constants.EDIT_CHATROOM_TYPE -> {

            }
            Constants.EDIT_MESSAGE_TYPE -> {

            }
            Constants.EDIT_EVENT_TYPE -> {

            }
            Constants.DELETE_CHATROOM_TYPE -> {
                // TODO: Push Notification
            }
            Constants.DELETE_MESSAGE_TYPE -> {

            }
            Constants.DELETE_EVENT_TYPE -> {
                // TODO: Push Notification
            }
            Constants.JOIN_USER_TYPE -> {
                // TODO: Push Notification
            }
            Constants.LEAVE_USER_TYPE -> {
                // TODO: Push Notification
            }
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

    private fun addDestructedMapToIntentExtras(intent: Intent, data: Map<String, String>) {
        intent.putExtra(Constants.DATA_KEYS, data.keys.toTypedArray())
        intent.putExtra(Constants.DATA_VALUES, data.values.toTypedArray())
    }

    private fun addDeletedModelIdToIntentExtras(intent: Intent, data: Map<String, String>) =
        intent.putExtra(Constants.DELETED_MODEL_ID, (data[Constants.ID] ?: error("Data ID must not be null!"))
            .toLong())

    private fun addParcelableUserToIntent(intent: Intent, data: Map<String, String>) {

    }

    private fun addParcelableMessageToIntent(intent: Intent, data: Map<String, String>) {

    }

    private fun addParcelableChatroomToIntent(intent: Intent, data: Map<String, String>) {

    }
}