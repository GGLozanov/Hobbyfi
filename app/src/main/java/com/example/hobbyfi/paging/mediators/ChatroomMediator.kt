package com.example.hobbyfi.paging.mediators

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType

@ExperimentalPagingApi
class ChatroomMediator(
    hobbyfiDatabase: HobbyfiDatabase,
    prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI,
    private val shouldFetchAuthChatroom: Boolean
) : ModelRemoteMediator<Int, Chatroom>(hobbyfiDatabase, prefConfig, hobbyfiAPI, RemoteKeyType.CHATROOM) {
    private val chatroomDao = hobbyfiDatabase.chatroomDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Chatroom>
    ): MediatorResult {

        // insert new page numbers (remote keys) after using cached page number to fetch new one

        return try {
            getChatrooms(loadType, state)
        } catch (exception: Exception) {
            try {
                Callbacks.dissectRepositoryExceptionAndThrow(exception)
            } catch(parsedEx: Exception) {
                MediatorResult.Error(parsedEx)
            }
            MediatorResult.Error(exception) // uncaught by repository handler error
        }
    }

    private suspend fun getChatrooms(loadType: LoadType, state: PagingState<Int, Chatroom>): MediatorResult {
        var page: Int? = null

        if(!shouldFetchAuthChatroom) {
            val pageKeyData = getKeyPageData(loadType, state)
            page = when (pageKeyData) {
                is MediatorResult.Success -> {
                    return pageKeyData
                }
                else -> {
                    pageKeyData as Int
                }
            }
        }

        val chatroomsResponse = hobbyfiAPI.fetchChatrooms(
            prefConfig.readToken()!!,
            page
        )

        val mediatorResult = if(shouldFetchAuthChatroom)
            saveChatroom(chatroomsResponse.modelList[0]) else saveChatrooms(chatroomsResponse, page!!, loadType)

        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatrooms_fetch_time)

        return mediatorResult
    }

    private suspend fun saveChatrooms(chatroomsResponse: CacheListResponse<Chatroom>, page: Int, loadType: LoadType): MediatorResult {
        val isEndOfList = chatroomsResponse.modelList.isEmpty() || shouldFetchAuthChatroom

        hobbyfiDatabase.withTransaction {
            // clear all rows in chatroom and remote keys table (for chatrooms)
            // FIXME: this cache timeout thing will surely not work
            if (loadType == LoadType.REFRESH || cacheTimedOut(R.string.pref_last_chatrooms_fetch_time)) {
                remoteKeysDao.deleteRemoteKeyByType(remoteKeyType)
                chatroomDao.deleteChatrooms()
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

        return MediatorResult.Success(endOfPaginationReached = isEndOfList)
    }

    private suspend fun saveChatroom(chatroom: Chatroom): MediatorResult {
        hobbyfiDatabase.withTransaction {
            remoteKeysDao.deleteRemoteKeyByType(remoteKeyType) // delete any saved chatrooms + remote keys
            chatroomDao.deleteChatrooms()
            chatroomDao.insert(chatroom) // insert first (and only) fetched chatroom
        }

        return MediatorResult.Success(endOfPaginationReached = true)
    }
}