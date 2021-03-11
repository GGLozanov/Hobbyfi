package com.example.hobbyfi.ui.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.hobbyfi.shared.Constants

interface ForegroundFCMReactivationListener: RefreshConnectionAware {
    val foregroundFCMReceiver: BroadcastReceiver
        get() = ForegroundFCMReactivationReceiver.getInstance(
            ::onForegroundReactivation, ::refreshDataOnConnectionRefresh
        )

    fun onForegroundReactivation(intent: Intent)

    class ForegroundFCMReactivationReceiver(
        private val onForegroundReactivation: (intent: Intent) -> Unit,
        private val refreshDataProcedure: () -> Unit
    ) : BroadcastReceiver() {

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
            private var instance: ForegroundFCMReactivationReceiver? = null

            @JvmName("getInstance1")
            fun getInstance(onForegroundReactivation: (intent: Intent) -> Unit, refreshDataProcedure: () -> Unit):
                    ForegroundFCMReactivationReceiver {
                synchronized(this) {
                    if(instance == null) {
                        instance = ForegroundFCMReactivationReceiver(onForegroundReactivation, refreshDataProcedure)
                    }

                    return instance as ForegroundFCMReactivationReceiver
                }
            }
        }
    }
}