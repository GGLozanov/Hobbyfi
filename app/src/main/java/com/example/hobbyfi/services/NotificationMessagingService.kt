package com.example.hobbyfi.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// TODO: Architecture built only on data notifications with push notifications created locally here
class NotificationMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i("NotificationMService", "WOOOOOOOOOO, FCM MESSAGE RECEIVED BEYBEEEE: ${message}")
        // message has `type` key in data payload that specifies the message type (ex: `EDIT_CHATROOM`, `DELETE_CHATROOM`, etc.)

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
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}