package com.example.hobbyfi.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// TODO: Architecture built only on data notifications with push notifications created locally here
class NotificationMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // update chatroom message => has ONLY updated fields in notification
        // if update chatroom message is last event id change update the cache and don't do anything special; else => show toast
        // event create notification should be triggered after that
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}