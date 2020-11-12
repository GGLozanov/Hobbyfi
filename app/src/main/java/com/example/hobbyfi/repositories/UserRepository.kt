package com.example.hobbyfi.repositories

import androidx.lifecycle.LiveData
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig

// fetches both auth users & chatroom users (map<string, user>)
class UserRepository(prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase) {
    // return livedata of pagedlist for users
    fun getUsers(pagingConfig: PagingConfig = Constants.getDefaultPageConfig()): LiveData<PagingData<User>> {
//        return Pager(
//            config = pagingConfig,
//            pagingSourceFactory = { hobbyfiDatabase.userDao.getUsers() }, // TODO: DI
//            remoteMediator =
//        ).liveData
        throw Exception() // stub
    }
}