package com.example.hobbyfi.repositories

import androidx.lifecycle.LiveData
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.persistence.MessageDao
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import java.lang.Exception

class MessageRepository(
    pagingSource: PagingSource<Int, Message>, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase) {
    // return livedata of pagedlist for messages
    fun getMessages(pagingConfig: PagingConfig = Constants.getDefaultPageConfig()): LiveData<PagingData<Chatroom>> {
//        return Pager(
//            config = pagingConfig,
//            pagingSourceFactory = { pagingSource }, // TODO: DI
//            remoteMediator = MessageMediator()
//        ).liveData
        throw Exception() // stub
    }
}