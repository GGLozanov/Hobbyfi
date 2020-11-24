package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.paging.*
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User
import com.example.hobbyfi.paging.mediators.UserMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.UserResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.isConnected
import com.example.hobbyfi.utils.TokenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

// fetches both auth users & chatroom users (map<string, user>)
class UserRepository @ExperimentalPagingApi constructor(
    private val remoteMediator: UserMediator, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {

    suspend fun getUser(): Flow<User?> {
        return withContext(Dispatchers.IO) {
            Log.i("UserRepository", "getUser -> getting current user")
            return@withContext object : NetworkBoundFetcher<User, UserResponse>() {
                override suspend fun saveNetworkResult(response: UserResponse) {
                    saveUser(response.model)
                }

                override fun shouldFetch(cache: User?): Boolean {
                    val lastUserFetchTime = prefConfig.readLastUserFetchTime()
                    return cache == null ||
                            ((System.currentTimeMillis() / 1000) - lastUserFetchTime) >= Constants.CACHE_TIMEOUT || connectivityManager.isConnected()
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun loadFromDb(): Flow<User?> {
                    val userId = TokenUtils.getTokenUserIdFromPayload(prefConfig.readToken())
                    return hobbyfiDatabase.userDao().getUserById(userId)
                }

                override suspend fun fetchFromNetwork(): UserResponse? {
                    return try {
                        val token = prefConfig.readToken()
                        if(token != null) hobbyfiAPI.fetchUser(token) else null
                    } catch(ex: Exception) {
                        try {
                            Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
                        } catch(authEx: AuthorisedRequestException) {
                            val token = getNewTokenWithRefresh()
                            fetchFromNetwork()
                        }
                    }
                }
            }.asFlow()
        }
    }

    suspend fun editUser(userFields: Map<String?, String?>): Response? {
        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")
            return@withContext try {
                hobbyfiAPI.editUser(prefConfig.readToken()!!, userFields)
            } catch(ex: Exception) {
                try {
                    Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
                } catch(authEx: AuthorisedRequestException) {
                    val token = getNewTokenWithRefresh()
                    editUser(userFields) // recursive call to this again; if everything goes as planned, this should never cause a recursive loop
                }
            }
        }
    }

    suspend fun deleteUser(): Response? {
        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")
            return@withContext try {
                hobbyfiAPI.deleteUser(prefConfig.readToken()!!) // TODO: Error handle for token value = "invalid"?
            } catch(ex: Exception) {
                try {
                    Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
                } catch(authEx: AuthorisedRequestException) {
                    val token = getNewTokenWithRefresh()
                        // if this ^ throws exception => user reauth; invalid refresh token & can't fetch response
                    deleteUser()
                }
            }
        }
    }

    // return livedata of pagedlist for users
    suspend fun getUsers(pagingConfig: PagingConfig = Constants.getDefaultPageConfig()): LiveData<PagingData<User>> {
        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")

//        return Pager(
//            config = pagingConfig,
//            pagingSourceFactory = { pagingSource }, // TODO: DI
//            remoteMediator =
//        ).liveData
            throw Exception() // stub
        }
    }

    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) { hobbyfiDatabase.userDao().insert(user) }
}