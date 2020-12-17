package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.paging.*
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.paging.mediators.MessageMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import java.lang.Exception

class MessageRepository @ExperimentalPagingApi constructor(
    private val remoteMediator: MessageMediator, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
    // return livedata of pagedlist for messages
    fun getMessages(pagingConfig: PagingConfig = Constants.getDefaultChatroomPageConfig()): LiveData<PagingData<Chatroom>> {
//        return Pager(
//            config = pagingConfig,
//            pagingSourceFactory = { pagingSource }, // TODO: DI
//            remoteMediator = MessageMediator()
//        ).liveData
        throw Exception() // stub
    }
}