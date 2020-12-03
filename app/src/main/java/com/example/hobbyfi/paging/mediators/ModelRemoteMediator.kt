package com.example.hobbyfi.paging.mediators

import androidx.paging.*
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.shared.RemoteKeyType
import java.io.InvalidObjectException

@ExperimentalPagingApi
abstract class CacheResponseRemoteMediator<Key: Any, Value: CacheResponse<*>>(
    private val hobbyfiDatabase: HobbyfiDatabase,
    private val remoteKeyType: RemoteKeyType
) : RemoteMediator<Key, Value>() {
    /**
     * this returns the page key or the final end of list success result
     */
    protected suspend fun getKeyPageData(loadType: LoadType, state: PagingState<Key, Value>): Any? {
        return when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getClosestRemoteKey(state)
                remoteKeys?.nextKey?.minus(1) ?: DEFAULT_PAGE_INDEX
            }
            LoadType.APPEND -> {
                val remoteKeys = getLastRemoteKey(state)
                    ?: throw InvalidObjectException("Remote key should not be null for $loadType")
                remoteKeys.nextKey
            }
            LoadType.PREPEND -> {
                val remoteKeys = getFirstRemoteKey(state)
                    ?: throw InvalidObjectException("Invalid state, key should not be null")
                //end of list condition reached
                remoteKeys.prevKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                remoteKeys.prevKey
            }
        }
    }

    /**
     * get the last remote key inserted which had the data
     */
    protected suspend fun getLastRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { chatroom -> hobbyfiDatabase.remoteKeysDao().getRemoteKeysByIdAndType(chatroom.id, RemoteKeyType.CHATROOM) }
    }

    /**
     * get the first remote key inserted which had the data
     */
    protected suspend fun getFirstRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .firstOrNull() { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { doggo -> appDatabase.getRepoDao().remoteKeysDoggoId(doggo.id) }
    }

    /**
     * get the closest remote key inserted which had the data
     */
    protected suspend fun getClosestRemoteKey(state: PagingState<Int, Chatroom>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { repoId ->
                appDatabase.getRepoDao().remoteKeysDoggoId(repoId)
            }
        }
    }
}