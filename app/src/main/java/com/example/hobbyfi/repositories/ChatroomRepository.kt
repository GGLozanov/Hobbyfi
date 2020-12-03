package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import androidx.paging.*
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.paging.mediators.ChatroomMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants.getDefaultPageConfig
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatroomRepository @ExperimentalPagingApi constructor(
    private val remoteMediator: ChatroomMediator,
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
    // return livedata of pagedlist for chatrooms
    @ExperimentalPagingApi
    fun getChatrooms(pagingConfig: PagingConfig = getDefaultPageConfig()): Flow<PagingData<Chatroom>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { hobbyfiDatabase.chatroomDao().getChatrooms() }, // TODO: DI & cache as SSOT
            remoteMediator = remoteMediator
        ).flow
    }

    // called when user joins their chatroom for the first time after recyclerview
    suspend fun deleteChatrooms(authChatroomId: Long) {
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_chatrooms_fetch_time)
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.withTransaction {
                hobbyfiDatabase.chatroomDao().deleteChatroomsExceptId(authChatroomId)
                hobbyfiDatabase.remoteKeysDao().deleteRemoteKeysForIdAndType(authChatroomId, RemoteKeyType.CHATROOM)
            }
        }
    }
}