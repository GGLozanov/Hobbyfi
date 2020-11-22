package com.example.hobbyfi.fetchers

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// implements caching policy for user & event fetch (i.e. caching NOT controlled by Paging3 or Coil)
abstract class NetworkBoundFetcher<CacheModel, NetworkResponse> {
    fun asFlow() = flow {
        emit(null)

        val dbValue = loadFromDb()
        emitAll(dbValue)

        if (shouldFetch(dbValue.first())) {
            val apiResponse = fetchFromNetwork() // if exception is thrown here, execution stops, user is notified, and initial db value is still emitted
            saveNetworkResult(apiResponse)
            emitAll(loadFromDb())
        }
    }

    @WorkerThread
    protected abstract suspend fun saveNetworkResult(item: NetworkResponse)

    @MainThread
    protected abstract fun shouldFetch(data: CacheModel?): Boolean

    @MainThread
    protected abstract fun loadFromDb(): Flow<CacheModel>

    @MainThread
    protected abstract suspend fun fetchFromNetwork(): NetworkResponse // handles/throws network exceptions

    @MainThread
    protected abstract fun isNetworkResponseInvalid(response: NetworkResponse): Boolean
}