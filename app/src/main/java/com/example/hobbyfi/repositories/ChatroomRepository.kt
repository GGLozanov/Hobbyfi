package com.example.hobbyfi.repositories

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.paging.sources.ChatroomPagingSource
import com.example.hobbyfi.persistence.ChatroomDao
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants.getDefaultPageConfig
import com.example.hobbyfi.shared.PrefConfig

class ChatroomRepository(prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase) {
    // return livedata of pagedlist for chatrooms
    fun getChatrooms(pagingConfig: PagingConfig = getDefaultPageConfig()): LiveData<PagingData<Chatroom>> {
//        return Pager(
//            config = pagingConfig,
//            pagingSourceFactory = { hobbyfiDatabase.chatroomDao.getChatrooms() }, // TODO: DI & cache as SSOT
//            remoteMediator = ChatroomMediator()
//        ).liveData
        throw Exception() // stub
    }
}