package com.example.hobbyfi.paging.mediators

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType
import kotlinx.coroutines.*

@ExperimentalPagingApi
class ChatroomMediator(
    hobbyfiDatabase: HobbyfiDatabase,
    prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI,
    private val shouldFetchAuthChatrooms: Boolean,
    private val authChatroomIds: List<Long>?
) : ModelMediator<Int, Chatroom>(
    hobbyfiDatabase, prefConfig,
    hobbyfiAPI, RemoteKeyType.CHATROOM
) {
    private val chatroomDao = hobbyfiDatabase.chatroomDao()

    override val cachePrefId: Int
        get() = R.string.pref_last_chatrooms_fetch_time

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH // keep default impl here & manually handle cache
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Chatroom>
    ): MediatorResult {
        // insert new page numbers (remote keys) after using cached page number to fetch new one
        Log.i("ChatroomMediator", "Loading all chatrooms based on shouldFetchAuthChatroom set to ${shouldFetchAuthChatrooms}")

        return try {
            fetchChatrooms(loadType, state)
        } catch (ex: Exception) {
            ex.printStackTrace()
            try {
                Repository.dissectExceptionAndThrow(ex)
            } catch(parsedEx: Exception) {
                MediatorResult.Error(parsedEx)
            }
            MediatorResult.Error(ex) // uncaught by repository handler error
        }
    }

    private suspend fun fetchChatrooms(loadType: LoadType, state: PagingState<Int, Chatroom>): MediatorResult {
        val page = getPage(loadType, state).let {
            when(it) {
                is MediatorResult.Success -> {
                    return@fetchChatrooms it
                }
                else -> {
                    it as Int
                }
            }
        }

        Log.i("ChatroomMediator", "Fetching next chatrooms with page ${page}")

        val chatroomsResponse = if(!shouldFetchAuthChatrooms) hobbyfiAPI.fetchChatrooms(
            prefConfig.getAuthUserToken()!!,
            page
        ) else hobbyfiAPI.fetchAuthChatrooms(prefConfig.getAuthUserToken()!!, page)

        val mediatorResult = saveChatrooms(chatroomsResponse, page, loadType)

        prefConfig.writeLastPrefFetchTimeNow(cachePrefId)

        return mediatorResult
    }

    private suspend fun saveChatrooms(chatroomsResponse: CacheListResponse<Chatroom>, page: Int, loadType: LoadType): MediatorResult {
        val isEndOfList = chatroomsResponse.modelList.isEmpty()
        Log.i("ChatroomMediator", "Fetched Chatrooms.")
        Log.i("ChatroomMediator", "Reached end of list: ${isEndOfList}")
        Log.i("ChatroomMediator", "Chatroom list: ${chatroomsResponse.modelList}")

        hobbyfiDatabase.withTransaction {
            val cacheTimeOut = Constants.cacheTimedOut(prefConfig, cachePrefId)
            // clear all rows in chatroom and remote keys table (for chatrooms)
            if (loadType == LoadType.REFRESH || cacheTimeOut) {
                Log.i("ChatroomMediator", "CHATROOM triggered refresh OR cache TIMEOUT. Clearing cache")
                clearCachedChatroomsByFetchType()
            }

            val keys = mapRemoteKeysFromModelList(chatroomsResponse.modelList, page, isEndOfList)
            Log.i("ChatroomMediator", "CHATROOM RemoteKeys created. RemoteKeys: ${keys}")
            Log.i("ChatroomMediator", "Inserting ChatroomList and RemoteKeys")
            remoteKeysDao.upsert(keys)
            chatroomDao.upsert(chatroomsResponse.modelList)
        }

        return MediatorResult.Success(endOfPaginationReached = isEndOfList)
    }

    private suspend fun clearCachedChatroomsByFetchType() {
        if(shouldFetchAuthChatrooms) {
            Log.i("ChatroomMediator", "ChatroomMediator deleting clear cached chatrooms for shouldFetcHaUTHcHATROOMS TRUE")
            authChatroomIds?.let {
                chatroomDao.deleteChatroomsByIds(it)
                remoteKeysDao.deleteRemoteKeysByTypeAndIds(mainRemoteKeyType, it)
            }
        } else {
            Log.i("ChatroomMediator", "ChatroomMediator deleting clear cached chatrooms for shouldFetcHaUTHcHATROOMS FALSE")
            if(authChatroomIds != null) {
                chatroomDao.deleteChatroomsNotPresentInIds(authChatroomIds)
                remoteKeysDao.deleteRemoteKeysByTypeAndNotPresentInIds(mainRemoteKeyType, authChatroomIds)
            } else {
                chatroomDao.deleteChatrooms()
                remoteKeysDao.deleteRemoteKeys()
            }
        }
    }

    override fun mapRemoteKeysFromModelList(
        modelList: List<Chatroom>,
        page: Int,
        isEndOfList: Boolean
    ): List<RemoteKeys> {
        val prevKey = if (page == DEFAULT_PAGE_INDEX) null else page - 1
        val nextKey = if (isEndOfList) null else page + 1
        Log.i("ChatroomMediator", "RemoteKeys calculated. Previous page: ${prevKey}; Next page: ${nextKey}")
        return modelList.map {
            RemoteKeys(id = it.id, prevKey = prevKey, nextKey = nextKey,
                modelType = if(shouldFetchAuthChatrooms) RemoteKeyType.AUTH_CHATROOM else mainRemoteKeyType
            )
        }
    }

    override suspend fun getRemoteKeysByIdAndType(modelId: Long): RemoteKeys? {
        Log.i("ChatroomMediator", "Chatroom id: $modelId and auth chatroom ids: $authChatroomIds")
        return if(shouldFetchAuthChatrooms) {
            authChatroomIds?.let {
                remoteKeysDao.getRemoteKeysByTypeAndIds(modelId, authChatroomIds, RemoteKeyType.AUTH_CHATROOM)
            }
        } else {
            if(authChatroomIds != null) {
                remoteKeysDao.getRemoteKeysByTypeAndNotPresentInIds(modelId, authChatroomIds, mainRemoteKeyType)
            } else {
                remoteKeysDao.getRemoteKeysByIdAndType(modelId, mainRemoteKeyType)
            }
        }
    }
}