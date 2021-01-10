package com.example.hobbyfi.shared

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.hobbyfi.viewmodels.base.BaseViewModel

abstract class BroadcastReceiverFactory {
    protected fun createReceiver(intentAction: String, onCorrectAction: (intent: Intent) -> Unit, onReceiveLog: String? = null,
                       onNoNotifyLog: String? = null, isNotChatroomOwnerOrShouldSee: ((intent: Intent) -> Boolean)): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == intentAction) {
                // people do checks here ^; idk why given the intent filter
                if (onReceiveLog != null) {
                    Log.i((context as Activity)::class.simpleName, onReceiveLog)
                }

                // if nothing passed => even the chatroom owner and/or auth user will be notified!
                if(isNotChatroomOwnerOrShouldSee(intent)) {
                    onCorrectAction(intent)
                } else {
                    if (onNoNotifyLog != null) {
                        Log.i((context as Activity)::class.simpleName, onNoNotifyLog)
                    }
                }
            }
        }
    }

    abstract fun createActionatedReceiver(action: String): BroadcastReceiver
}