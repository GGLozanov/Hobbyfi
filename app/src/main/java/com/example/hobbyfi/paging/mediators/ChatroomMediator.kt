package com.example.hobbyfi.paging.mediators

import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        Log.i("ChatroomMediator", "Loading all chatrooms based on shouldFetchAuthChatroom set to ${shouldFetchAuthChatroom}")

        return try {
            getChatrooms(loadType, state)
        } catch (exception: Exception) {
            exception.printStackTrace()
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
                    Log.i("ChatroomMediator", "PREPEND triggered MediatorResult.Success. Returning")
                    return pageKeyData
                }
                else -> {
                    pageKeyData as Int
                }
            }
        }

        Log.i("ChatroomMediator", "Fetching next chatrooms with page ${page}")

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
        Log.i("ChatroomMediator", "Fetched Chatrooms.")
        Log.i("ChatroomMediator", "Reached end of list: ${isEndOfList}")
        Log.i("ChatroomMediator", "Chatroom list: ${chatroomsResponse.modelList}")

        hobbyfiDatabase.withTransaction {
            // clear all rows in chatroom and remote keys table (for chatrooms)
            // FIXME: this cache timeout thing will surely not work
            val cacheTimedOut = cacheTimedOut(R.string.pref_last_chatrooms_fetch_time)
            if (loadType == LoadType.REFRESH || cacheTimedOut) {
                Log.i("ChatroomMediator", "Chatroom triggered refresh or timeout cache. Clearing cache. WasCacheTimedOut: ${cacheTimedOut}")
                remoteKeysDao.deleteRemoteKeyByType(remoteKeyType)
                chatroomDao.deleteChatrooms()
            }
            val prevKey = if (page == DEFAULT_PAGE_INDEX) null else page - 1
            val nextKey = if (isEndOfList) null else page + 1
            Log.i("ChatroomMediator", "Chatroom RemoteKeys calculated. Previous page: ${prevKey}; Next page: ${nextKey}")
            val keys = chatroomsResponse.modelList.map {
                RemoteKeys(id = it.id, prevKey = prevKey, nextKey = nextKey,
                    modelType = remoteKeyType
                )
            }
            Log.i("ChatroomMediator", "Chatroom RemoteKeys created. RemoteKeys: ${keys}")
            Log.i("ChatroomMediator", "Inserting ChatroomList and RemoteKeys")
            remoteKeysDao.insertList(keys)
            chatroomDao.insertList(chatroomsResponse.modelList)
        }

        return MediatorResult.Success(endOfPaginationReached = isEndOfList)
    }

    private suspend fun saveChatroom(chatroom: Chatroom): MediatorResult {
        Log.i("ChatroomMediator", "Received chatroom: ${chatroom}")

        hobbyfiDatabase.withTransaction {
            Log.i("ChatroomMediator", "Deleting RemoteKeys and cached chatrooms")
            remoteKeysDao.deleteRemoteKeyByType(remoteKeyType) // delete any saved chatrooms + remote keys
            chatroomDao.deleteChatrooms()
            chatroomDao.insert(chatroom) // insert first (and only) fetched chatroom
        }

        return MediatorResult.Success(endOfPaginationReached = true)
    }
}