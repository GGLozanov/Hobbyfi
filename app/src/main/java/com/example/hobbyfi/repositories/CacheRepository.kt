package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.isConnected
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

abstract class CacheRepository(
    prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI,
    protected val hobbyfiDatabase: HobbyfiDatabase,
    protected val connectivityManager: ConnectivityManager,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Repository(prefConfig, hobbyfiAPI, coroutineDispatcher) {
    protected val firestore = Firebase.firestore

    protected fun<T> adheresToDefaultCachePolicy(cache: T?, cachePref: Int): Boolean = cache == null ||
                Constants.cacheTimedOut(prefConfig, cachePref) || connectivityManager.isConnected()
}