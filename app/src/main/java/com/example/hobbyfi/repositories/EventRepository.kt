package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.PrefConfig

class EventRepository(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI,
    hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
}