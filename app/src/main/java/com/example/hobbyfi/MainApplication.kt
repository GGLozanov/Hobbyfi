package com.example.hobbyfi

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.repositories.*
import com.example.hobbyfi.shared.PrefConfig
import com.facebook.CallbackManager
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.tasks.OnFailureListener
import io.branch.referral.Branch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.*

class MainApplication : MultiDexApplication(), KodeinAware {
    @ExperimentalPagingApi
    override val kodein: Kodein = Kodein.lazy {
        import(androidXModule(this@MainApplication))

        // todo: remove these service bindings and replace them with the bindings from Kodein's module.kt
        bind(tag = "connectivityManager") from singleton { applicationContext!!.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
        bind(tag = "locationManager") from singleton { applicationContext!!.getSystemService(
            Context.LOCATION_SERVICE) as LocationManager
        }
        bind(tag = "localBroadcastManager") from singleton {
            LocalBroadcastManager.getInstance(applicationContext!!)
        }
        bind(tag = "api") from singleton { HobbyfiAPI.invoke(instance(tag = "connectivityManager")) }
        bind(tag = "database") from singleton { HobbyfiDatabase.getInstance(context = applicationContext) }
        bind(tag = "prefConfig") from singleton { PrefConfig(applicationContext) }
        bind(tag = "tokenRepository") from singleton { TokenRepository(
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI
            )
        }
        bind(tag = "userRepository") from singleton { UserRepository(
            instance(tag = "prefConfig") as PrefConfig,
            instance(tag = "api") as HobbyfiAPI,
            instance(tag = "database") as HobbyfiDatabase,
            instance(tag = "connectivityManager") as ConnectivityManager
        )
        }
        bind(tag = "chatroomRepository") from singleton { ChatroomRepository(
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase,
                instance(tag = "connectivityManager") as ConnectivityManager
            )
        }
        bind(tag = "eventRepository") from singleton { EventRepository(
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase,
                instance(tag = "connectivityManager") as ConnectivityManager
            )
        }
        bind(tag = "messageRepository") from singleton { MessageRepository(
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase,
                instance(tag = "connectivityManager") as ConnectivityManager
            )
        }
        bind(tag = "callbackManager") from singleton {
            CallbackManager.Factory.create()
        }
    }

    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
        MainApplication.applicationContext = applicationContext

        ProviderInstaller.installIfNeeded(applicationContext)
        // Branch logging for debugging
        Branch.enableLogging()

        // Branch object initialization
        Branch.setPlayStoreReferrerCheckTimeout(0)
        Branch.getAutoInstance(this).enableFacebookAppLinkCheck()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        lateinit var applicationContext: Context
    }
}