package com.example.hobbyfi.repositories

import androidx.lifecycle.LiveData
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.persistence.MessageDao
import com.example.hobbyfi.shared.Constants
import java.lang.Exception

class MessageRepository(hobbyfiDatabase: HobbyfiDatabase) : CacheRepository(hobbyfiDatabase) {
    // return livedata of pagedlist for messages
    fun getMessages(pagingConfig: PagingConfig = Constants.getDefaultPageConfig()): LiveData<PagingData<Chatroom>> {
//        return Pager(
//            config = pagingConfig,
//            pagingSourceFactory = { hobbyfiDatabase.messageDao.getMessages() }, // TODO: DI
//            remoteMediator = MessageMediator()
//        ).liveData
        throw Exception() // stub
    }
}