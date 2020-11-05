package com.example.hobbyfi

import android.app.Application

// TODO: DI, Notification channel, etc. setup
class MainApplication : Application() {
    // register prefconfig, appdb, repositories, & maybe even fragments (?) as singletons
    override fun onCreate() {
        super.onCreate()
    }
}