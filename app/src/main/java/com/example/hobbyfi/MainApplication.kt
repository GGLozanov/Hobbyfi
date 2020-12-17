package com.example.hobbyfi

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.core.util.Predicate
import androidx.multidex.MultiDexApplication
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import androidx.room.Room
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.paging.mediators.ChatroomMediator
import com.example.hobbyfi.paging.mediators.MessageMediator
import com.example.hobbyfi.paging.mediators.UserMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.repositories.*
import com.example.hobbyfi.shared.PrefConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.gson.GsonBuilder
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

// TODO: Notification channel setup
@ExperimentalPagingApi
class MainApplication : MultiDexApplication(), KodeinAware {
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
                instance(tag = "userMediator") as UserMediator,
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
                instance(tag = "messageMediator") as MessageMediator,
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI,
                instance(tag = "database") as HobbyfiDatabase,
                instance(tag = "connectivityManager") as ConnectivityManager
            )
        }
//        bind(tag = "chatroomMediator") from singleton { ChatroomMediator(
//                instance(tag = "database") as HobbyfiDatabase,
//                instance(tag = "prefConfig") as PrefConfig,
//                instance(tag = "api") as HobbyfiAPI
//            )
//        } // FIXME: symmetry with mediator DI
        bind(tag = "messageMediator") from singleton { MessageMediator(
                instance(tag = "database") as HobbyfiDatabase,
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI
            )
        }
        bind(tag = "userMediator") from singleton { UserMediator(
                instance(tag = "database") as HobbyfiDatabase,
                instance(tag = "prefConfig") as PrefConfig,
                instance(tag = "api") as HobbyfiAPI
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}