package com.example.hobbyfi.repositories

import androidx.lifecycle.LiveData
import androidx.paging.*
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom import com.example.hobbyfi.persistence.ChatroomDao
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants.getDefaultPageConfig
import com.example.hobbyfi.shared.PrefConfig

class ChatroomRepository(
    pagingSource: PagingSource<Int, Chatroom>,
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase) {
    // return livedata of pagedlist for chatrooms
    fun getChatrooms(pagingConfig: PagingConfig = getDefaultPageConfig()): LiveData<PagingData<Chatroom>> {
//        return Pager(
//            config = pagingConfig,
//            pagingSourceFactory = { pagingSource }, // TODO: DI & cache as SSOT
//            remoteMediator = ChatroomMediator()
//        ).liveData
        throw Exception() // stub
    }
}