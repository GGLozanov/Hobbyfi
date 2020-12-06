package com.example.hobbyfi.paging.mediators

import android.util.Log
import androidx.paging.*
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.persistence.RemoteKeysDao
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * get the last remote key inserted which had the data
     */
    protected suspend fun getLastRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { model -> remoteKeysDao.getRemoteKeysByIdAndType(model.id, remoteKeyType) }
    }

    /**
     * get the first remote key inserted which had the data
     */
    protected suspend fun getFirstRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { model -> remoteKeysDao.getRemoteKeysByIdAndType(model.id, remoteKeyType) }
    }

    /**
     * get the closest remote key inserted which had the data
     */
    protected suspend fun getClosestRemoteKey(state: PagingState<Key, Value>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { modelId ->
                remoteKeysDao.getRemoteKeysByIdAndType(modelId, remoteKeyType)
            }
        }
    }
}