package com.example.hobbyfi

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.multidex.MultiDexApplication
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.paging.mediators.ChatroomMediator
import com.example.hobbyfi.paging.mediators.MessageMediator
import com.example.hobbyfi.paging.mediators.UserMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.repositories.*
import com.example.hobbyfi.shared.PrefConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

// TODO: DI, Notification channel, etc. setup
@ExperimentalPagingApi
class MainApplication : MultiDexApplication(), KodeinAware {
    // register prefconfig, appdb, repositories, datasources, remotemediators & maybe even fragments (?) as singletons
    @RequiresApi(Build.VERSION_CODES.N)
    override val kodein: Kodein = Kodein.lazy {
        import(androidXModule(this@MainApplication))

        // todo: put these strings in constants obj
        bind(tag = "connectivityManager") from singleton { applicationContext!!.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
                instance(tag = "database") as HobbyfiDatabase
            )
        }
        bind(tag = "chatroomRepository") from singleton { ChatroomRepository(
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase
            )
        }
        bind(tag = "eventRepository") from singleton { EventRepository(
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase
            )
        }
        bind(tag = "messageRepository") from singleton { MessageRepository(
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase
            )
        }
        bind(tag = "chatroomMediator") from singleton { ChatroomMediator(
                instance(tag = "chatroomRepository") as ChatroomRepository
            )
        }
        bind(tag = "messageMediator") from singleton { MessageMediator(
                instance(tag = "messageMediator") as MessageRepository,
                instance(tag = "prefConfig") as PrefConfig
            )
        }
        bind(tag = "userMediator") from singleton { UserMediator(
                instance(tag = "userRepository") as UserRepository,
                instance(tag = "prefConfig") as PrefConfig
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}