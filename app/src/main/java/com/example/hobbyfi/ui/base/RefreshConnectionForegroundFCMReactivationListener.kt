package com.example.hobbyfi.ui.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.hobbyfi.shared.Constants

interface RefreshConnectionForegroundFCMReactivationListener: ForegroundFCMReactivationListener, RefreshConnectionAware {
    override val foregroundFCMReceiver: BroadcastReceiver
        get() = RefreshConnectionForegroundFCMReactivationReceiver.getInstance(
            ::onForegroundReactivation, ::refreshDataOnConnectionRefresh
        )

    class RefreshConnectionForegroundFCMReactivationReceiver(
        private val onForegroundReactivation: (intent: Intent) -> Unit,
        private val refreshDataProcedure: () -> Unit
    ) : ForegroundFCMReactivationListener.ForegroundFCMReactivationReceiver(onForegroundReactivation) {

        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == Constants.FOREGROUND_REACTIVIATION_ACTION) {
                onForegroundReactivation(intent)
                refreshDataProcedure()
            } else {
                Log.w(context::class.simpleName, "Foreground reactivation for App Standby broadcastreceiver called with incorrect action!")
            }
        }

        companion object {
            @Volatile
            private var instance: RefreshConnectionForegroundFCMReactivationReceiver? = null

            @JvmName("getInstance1")
            fun getInstance(onForegroundReactivation: (intent: Intent) -> Unit, refreshDataProcedure: () -> Unit):
                    RefreshConnectionForegroundFCMReactivationReceiver {
                synchronized(this) {
                    if(instance == null) {
                        instance = RefreshConnectionForegroundFCMReactivationReceiver(onForegroundReactivation, refreshDataProcedure)
                    }

                    return instance as RefreshConnectionForegroundFCMReactivationReceiver
                }
            }
        }
    }
}