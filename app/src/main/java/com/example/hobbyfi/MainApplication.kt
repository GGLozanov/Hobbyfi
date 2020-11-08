package com.example.hobbyfi

import android.app.Application
import androidx.multidex.MultiDexApplication

// TODO: DI, Notification channel, etc. setup
class MainApplication : MultiDexApplication() {
    // register prefconfig, appdb, repositories, datasources, remotemediators & maybe even fragments (?) as singletons
    override fun onCreate() {
        super.onCreate()
    }
}