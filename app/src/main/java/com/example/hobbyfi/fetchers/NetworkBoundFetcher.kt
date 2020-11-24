package com.example.hobbyfi.fetchers

import android.util.Log
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

        val dbValue = loadFromDb().first()
        emit(dbValue)

        if (shouldFetch(dbValue)) {
            val apiResponse = fetchFromNetwork() // if exception is thrown here, execution stops, user is notified, and initial db value is still emitted
            if(apiResponse != null) {
                saveNetworkResult(apiResponse)
                emitAll(loadFromDb())
            } else {
                Log.w("NBF asFlow", "Couldn't fetch response and no exception  ")
            }
        }
    }

    @WorkerThread
    protected abstract suspend fun saveNetworkResult(response: NetworkResponse)

    @MainThread
    protected abstract fun shouldFetch(cache: CacheModel?): Boolean

    @MainThread
    protected abstract fun loadFromDb(): Flow<CacheModel?>

    @MainThread
    protected abstract suspend fun fetchFromNetwork(): NetworkResponse? // handles/throws network exceptions
}