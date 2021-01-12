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
) : ModelRemoteMediator<Int, Chatroom>(hobbyfiDatabase, prefConfig, hobbyfiAPI, RemoteKeyType.CHATROOM) {
    private val chatroomDao = hobbyfiDatabase.chatroomDao()

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
                Callbacks.dissectRepositoryExceptionAndThrow(ex)
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

        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatrooms_fetch_time)

        return mediatorResult
    }

    private suspend fun saveChatrooms(chatroomsResponse: CacheListResponse<Chatroom>, page: Int, loadType: LoadType): MediatorResult {
        val isEndOfList = chatroomsResponse.modelList.isEmpty()
        Log.i("ChatroomMediator", "Fetched Chatrooms.")
        Log.i("ChatroomMediator", "Reached end of list: ${isEndOfList}")
        Log.i("ChatroomMediator", "Chatroom list: ${chatroomsResponse.modelList}")

        hobbyfiDatabase.withTransaction {
            // clear all rows in chatroom and remote keys table (for chatrooms)
            val cacheTimedOut = Constants.cacheTimedOut(prefConfig, R.string.pref_last_chatrooms_fetch_time)
            if (loadType == LoadType.REFRESH || cacheTimedOut) {
                Log.i("ChatroomMediator", "CHATROOM triggered refresh or timeout cache. Clearing cache. WasCacheTimedOut: ${cacheTimedOut}")
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

    override suspend fun getLastRemoteKey(state: PagingState<Int, Chatroom>): RemoteKeys? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { model -> getRemoteKeysByFetchType(model.id) }
    }

    override suspend fun getFirstRemoteKey(state: PagingState<Int, Chatroom>): RemoteKeys? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { model -> getRemoteKeysByFetchType(model.id) }
    }

    override suspend fun getClosestRemoteKey(state: PagingState<Int, Chatroom>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { modelId -> getRemoteKeysByFetchType(modelId) }
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

    private suspend fun getRemoteKeysByFetchType(id: Long): RemoteKeys? {
        Log.i("ChatroomMediator", "Chatroom id: $id and auth chatroom ids: $authChatroomIds")
        return if(shouldFetchAuthChatrooms) {
            authChatroomIds?.let {
                remoteKeysDao.getRemoteKeysTypeAndIds(id, authChatroomIds, RemoteKeyType.AUTH_CHATROOM)
            }
        } else {
            if(authChatroomIds != null) {
                // LIMIT 1 OFFSET (SELECT COUNT(*) FROM remoteKeys WHERE modelType = :filterRemoteKeyType)
                // Log.i("ChatroomMediator", "Remote keys append: ${remoteKeysDao.getLastRemoteKeysByTypeAndOffsetFilter(id, mainRemoteKeyType)}")
                remoteKeysDao.getRemoteKeysByTypeAndNotPresentInIds(id, authChatroomIds, mainRemoteKeyType)
            } else {
                remoteKeysDao.getRemoteKeysByIdAndType(id, mainRemoteKeyType)
            }
        }
    }
}