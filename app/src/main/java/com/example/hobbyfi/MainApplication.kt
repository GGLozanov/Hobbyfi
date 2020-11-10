package com.example.hobbyfi

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule

// TODO: DI, Notification channel, etc. setup
class MainApplication : MultiDexApplication(), KodeinAware {
    // register prefconfig, appdb, repositories, datasources, remotemediators & maybe even fragments (?) as singletons
    override val kodein: Kodein = Kodein.lazy {
        import(androidXModule(this@MainApplication))

        // todo: put these strings in constants obj
//        bind(tag = "connectivityManager") from singleton { applicationContext!!.getSystemService(
//            Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
//        bind(tag = "api") from singleton { SPENDiDAPI.invoke(instance(tag = "connectivityManager")) }
//        bind(tag = "database") from singleton { AppDatabase.getInstance(context = applicationContext) }
//        bind(tag = "spendiDRepository") from singleton { SpendiDRepository(instance(tag = "api"), (instance(tag = "database") as AppDatabase)
//            .demographicsXDao(), (instance(tag = "database") as AppDatabase).budgetXDao()) }
    }

    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}