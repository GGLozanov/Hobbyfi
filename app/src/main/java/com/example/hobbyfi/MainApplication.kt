package com.example.hobbyfi

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.multidex.MultiDexApplication
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingSource
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.User
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
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

// TODO: DI, Notification channel, etc. setup
@ExperimentalPagingApi
class MainApplication : MultiDexApplication(), KodeinAware {
    @RequiresApi(Build.VERSION_CODES.N)
    override val kodein: Kodein = Kodein.lazy {
        import(androidXModule(this@MainApplication))

        // todo: put these strings in constants obj
        bind(tag = "connectivityManager") from singleton { applicationContext!!.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
        bind(tag = "locationManager") from singleton { applicationContext!!.getSystemService(
            Context.LOCATION_SERVICE) as LocationManager
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
                instance(tag = "userPagingSource") as PagingSource<Int, User>,
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase
            )
        }
        bind(tag = "chatroomRepository") from singleton { ChatroomRepository(
                instance(tag = "chatroomPagingSource") as PagingSource<Int, Chatroom>,
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
                instance(tag = "messagePagingSource") as PagingSource<Int, Message>,
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
        // TODO: Registering these as singletons may not work as they may need to be recreated in pagingSourceFactory
        // OR TODO: Might need to keep these as singletons. Heck if I know
//        bind(tag = "chatroomPagingSource") from provider {
//            (instance(tag = "database") as HobbyfiDatabase).chatroomDao().getChatrooms()
//        }
//        bind(tag = "messagePagingSource") from provider {
//            (instance(tag = "database") as HobbyfiDatabase).messageDao().getMessages()
//        }
//        bind(tag = "userPagingSource") from provider {
//            (instance(tag = "database") as HobbyfiDatabase).userDao().getUsers()
//        }
    }

    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}