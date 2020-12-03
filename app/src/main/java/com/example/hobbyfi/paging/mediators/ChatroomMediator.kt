package com.example.hobbyfi.paging.mediators

import android.net.ConnectivityManager
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType

@ExperimentalPagingApi
class ChatroomMediator(
    hobbyfiDatabase: HobbyfiDatabase,
    prefConfig: PrefConfig,
    private val hobbyfiAPI: HobbyfiAPI,
    private val connectivityManager: ConnectivityManager
) : ModelRemoteMediator<Int, Chatroom>(hobbyfiDatabase, prefConfig, RemoteKeyType.CHATROOM) {
    private val chatroomDao = hobbyfiDatabase.chatroomDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Chatroom>
    ): MediatorResult {
        val pageKeyData = getKeyPageData(loadType, state)
        val page = when (pageKeyData) {
            is MediatorResult.Success -> {
                return pageKeyData
            }
            else -> {
                pageKeyData as Int
            }
        }

        // insert new page numbers (remote keys) after using cached page number to fetch new one

        return try {
            val chatroomsResponse = hobbyfiAPI.fetchChatrooms(
                prefConfig.readToken()!!,
                page
            )
            val isEndOfList = chatroomsResponse.modelList.isEmpty()
            hobbyfiDatabase.withTransaction {
                // clear all rows in chatroom and remote keys table (for chatrooms)
                // FIXME: this cache timeout thing will surely not work
                if (loadType == LoadType.REFRESH || cacheTimedOut(R.string.pref_last_chatrooms_fetch_time)) {
                    remoteKeysDao.deleteRemoteKeyByType(remoteKeyType)
                    hobbyfiDatabase.chatroomDao().deleteChatrooms()
                }
                val prevKey = if (page == DEFAULT_PAGE_INDEX) null else page - 1
                val nextKey = if (isEndOfList) null else page + 1
                val keys = chatroomsResponse.modelList.map {
                    RemoteKeys(id = it.id, prevKey = prevKey, nextKey = nextKey,
                        modelType = remoteKeyType
                    )
                }
                remoteKeysDao.insertList(keys)
                chatroomDao.insertList(chatroomsResponse.modelList)
            }
            MediatorResult.Success(endOfPaginationReached = isEndOfList)
        } catch (exception: Exception) {
            try {
                Callbacks.dissectRepositoryExceptionAndThrow(exception)
            } catch(parsedEx: Exception) {
                MediatorResult.Error(parsedEx)
            }
            MediatorResult.Error(exception) // uncaught by repository handler error
        }
    }
}