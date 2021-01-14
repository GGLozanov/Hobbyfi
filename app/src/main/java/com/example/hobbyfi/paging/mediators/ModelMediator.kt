package com.example.hobbyfi.paging.mediators

import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.persistence.RemoteKeysDao
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType

@ExperimentalPagingApi
abstract class ModelMediator<Key: Any, Value: Model>(
    protected val hobbyfiDatabase: HobbyfiDatabase,
    protected val prefConfig: PrefConfig,
    protected val hobbyfiAPI: HobbyfiAPI,
    protected val mainRemoteKeyType: RemoteKeyType
) : RemoteMediator<Key, Value>() {
    companion object {
        val DEFAULT_PAGE_INDEX = 1
    }

    protected val remoteKeysDao: RemoteKeysDao = hobbyfiDatabase.remoteKeysDao()

    protected suspend fun getPage(loadType: LoadType, state: PagingState<Key, Value>): Any {
        return when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = hobbyfiDatabase.withTransaction {
                    getClosestRemoteKey(state)
                }
                Log.i("ModelRemoteM", "getKeyPageData => REFRESH Remote Keys: $remoteKeys")
                remoteKeys?.nextKey?.minus(1) ?: DEFAULT_PAGE_INDEX
            }
            LoadType.APPEND -> { // load type for whenever data needs to be appended to the paged list (scroll down)
                // can't return mediator result with endOfPaginationReached to true here because
                // this may be the last remote key but we don't have context for remote and rely on cache
                val remoteKeys = hobbyfiDatabase.withTransaction {
                    getLastRemoteKey(state)
                }
                Log.i("ModelRemoteM", "getKeyPageData => APPEND Remote Keys: $remoteKeys")
                if (remoteKeys?.nextKey == null) {
                    Log.i("ModelRemoteM", "getKeyPageData => REMOTE MEDIATOR TRIGGERED RETURN (END OF PAGINATION) FOR APPEND")
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                remoteKeys.nextKey
            }
            LoadType.PREPEND -> {
                // return MediatorResult.Success(endOfPaginationReached = true) // load type for whenever data needs to be prepended to the paged list (scroll up after down)
                val remoteKeys = hobbyfiDatabase.withTransaction {
                    getFirstRemoteKey(state)
                }
                // end of list condition reached -> reached the top of the page where the first page is loaded initially
                // which meanas we can set endOfPaginationReached to true
                Log.i("ModelRemoteM", "getKeyPageData => PREPEND Remote Keys: $remoteKeys")
                if(remoteKeys?.prevKey == null) {
                    Log.i("ModelRemoteM", "getKeyPageData => REMOTE MEDIATOR TRIGGERED RETURN (END OF PAGINATION) FOR PREPEND")
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                remoteKeys.prevKey
            }
        }
    }

    protected open suspend fun getLastRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { model -> remoteKeysDao.getRemoteKeysByIdAndType(model.id, mainRemoteKeyType) }
    }

    protected open suspend fun getFirstRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { model -> remoteKeysDao.getRemoteKeysByIdAndType(model.id, mainRemoteKeyType) }
    }

    protected open suspend fun getClosestRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { modelId ->
                remoteKeysDao.getRemoteKeysByIdAndType(modelId, mainRemoteKeyType)
            }
        }
    }

    protected open fun mapRemoteKeysFromModelList(modelList: List<Value>, page: Int, isEndOfList: Boolean): List<RemoteKeys> {
        val prevKey = if (page == DEFAULT_PAGE_INDEX) null else page - 1
        val nextKey = if (isEndOfList) null else page + 1
        Log.i("ModelRemoteM", "RemoteKeys calculated. Previous page: ${prevKey}; Next page: ${nextKey}")
        return modelList.map {
            RemoteKeys(id = it.id, prevKey = prevKey, nextKey = nextKey,
                modelType = mainRemoteKeyType
            )
        }
    }
}