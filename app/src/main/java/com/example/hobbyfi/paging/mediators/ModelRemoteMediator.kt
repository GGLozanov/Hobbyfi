package com.example.hobbyfi.paging.mediators

import androidx.paging.*
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.persistence.RemoteKeysDao
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType
import java.io.InvalidObjectException

@ExperimentalPagingApi
abstract class ModelRemoteMediator<Key: Any, Value: Model>(
    protected val hobbyfiDatabase: HobbyfiDatabase,
    protected val prefConfig: PrefConfig,
    protected val hobbyfiAPI: HobbyfiAPI,
    protected val remoteKeyType: RemoteKeyType
) : RemoteMediator<Key, Value>() {
    companion object {
        val DEFAULT_PAGE_INDEX = 1
    }

    protected val remoteKeysDao: RemoteKeysDao = hobbyfiDatabase.remoteKeysDao()

    /**
     * this returns the page key or the final end of list success result
     */
    protected suspend fun getKeyPageData(loadType: LoadType, state: PagingState<Key, Value>): Any? {
        return when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getClosestRemoteKey(state)
                remoteKeys?.nextKey?.minus(1) ?: DEFAULT_PAGE_INDEX
            }
            LoadType.APPEND -> { // load type for whenever data needs to be appended to the paged list (scroll down)
                // can't return mediator result with endOfPaginationReached to true here because
                // this may be the last remote key but we don't have context for remote and rely on cache
                val remoteKeys = getLastRemoteKey(state)
                    ?: throw InvalidObjectException("Remote key should not be null for $loadType")
                remoteKeys.nextKey
            }
            LoadType.PREPEND -> { // load type for whenever
                val remoteKeys = getFirstRemoteKey(state)
                    ?: throw InvalidObjectException("Invalid state, key should not be null")
                // end of list condition reached -> reached the top of the page where the first page is loaded initially
                // which meanas we can set endOfPaginationReached to true
                remoteKeys.prevKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                remoteKeys.prevKey
            }
        }
    }

    /**
     * get the last remote key inserted which had the data
     */
    protected fun getLastRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { model -> remoteKeysDao.getRemoteKeysByIdAndType(model.id, remoteKeyType) }
    }

    /**
     * get the first remote key inserted which had the data
     */
    protected fun getFirstRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .firstOrNull() { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { model -> remoteKeysDao.getRemoteKeysByIdAndType(model.id, remoteKeyType) }
    }

    /**
     * get the closest remote key inserted which had the data
     */
    protected fun getClosestRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { modelId ->
                remoteKeysDao.getRemoteKeysByIdAndType(modelId, remoteKeyType)
            }
        }
    }

    protected fun cacheTimedOut(prefId: Int): Boolean {
        return ((System.currentTimeMillis() / 1000) - prefConfig.readLastPrefFetchTime(prefId)) <= Constants.CACHE_TIMEOUT
    }
}