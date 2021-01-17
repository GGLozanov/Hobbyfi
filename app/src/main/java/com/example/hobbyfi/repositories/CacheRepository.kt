package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.persistence.BaseDao
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.isConnected
import com.example.hobbyfi.utils.TokenUtils
import com.facebook.AccessToken
import com.facebook.Profile
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

abstract class CacheRepository(
    prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI,
    protected val hobbyfiDatabase: HobbyfiDatabase, protected val connectivityManager: ConnectivityManager
) : Repository(prefConfig, hobbyfiAPI) {
    protected val firestore = Firebase.firestore

    protected fun<T> adheresToDefaultCachePolicy(cache: T?, cachePref: Int): Boolean = cache == null ||
                Constants.cacheTimedOut(prefConfig, cachePref) || connectivityManager.isConnected()
}