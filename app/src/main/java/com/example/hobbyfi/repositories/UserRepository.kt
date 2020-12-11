package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.*
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.User
import com.example.hobbyfi.paging.mediators.UserMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.*
import com.facebook.AccessToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// fetches both auth users & chatroom users
class UserRepository @ExperimentalPagingApi constructor(
    private val remoteMediator: UserMediator, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {

    suspend fun getUser(): Flow<User?> {
        Log.i("UserRepository", "getUser -> getting current user")
        return object : NetworkBoundFetcher<User, CacheResponse<User>>() {
            override suspend fun saveNetworkResult(response: CacheResponse<User>) {
                saveUser(response.model)
            }

            override fun shouldFetch(cache: User?): Boolean {
                Log.i("UserRepository", "getUser => isConnected: " + connectivityManager.isConnected())
                Log.i("UserRepository", "getUser => shouldFetch: " + (cache == null ||
                        Constants.cacheTimedOut(prefConfig, R.string.pref_last_user_fetch_time) || connectivityManager.isConnected()))
                return adheresToDefaultCachePolicy(cache, R.string.pref_last_user_fetch_time)
            }

            override suspend fun loadFromDb(): Flow<User?> {
                Log.i("UserRepository", "getUser -> ${prefConfig.readToken()}")
                return try {
                    val userId = getAuthUserIdFromToken()

                    hobbyfiDatabase.userDao().getUserById(userId)
                } catch(ex: Exception) {
                    try {
                        Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
                    } catch(authEx: AuthorisedRequestException) {
                        getNewTokenWithRefresh()
                        loadFromDb()
                    }
                }
            }

            // since loadFromDb() should aways be executed first
            // and that checks for expired token errors
            // this request should proceed without much/any fail for expired tokens, which is why they aren't validated here
            override suspend fun fetchFromNetwork(): CacheResponse<User>? {
                Log.i("UserRepository", "getUser => fetchFromNetwork() => fetching current auth user from network")
                return try {
                    val token = if(Constants.isFacebookUserAuthd()) AccessToken.getCurrentAccessToken().token else prefConfig.readToken()
                    if(token != null) {
                        val response = hobbyfiAPI.fetchUser(token)
                        Log.i("UserRepository", "getUser -> ${response?.model}")
                        response
                    } else null
                } catch(ex: Exception) {
                    ex.printStackTrace()
                    Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true) // no need for authorised request handling here because token parsing already handles expired token exceptions
                }
            }
        }.asFlow()
    }

    suspend fun editUser(userFields: Map<String?, String?>): Response? {
        Log.i("TokenRepository", "editUser -> editing current user")
        return try {
            val userId = getAuthUserIdFromToken() // validate token expiry by attempting to get id
            hobbyfiAPI.editUser(
                if(Constants.isFacebookUserAuthd()) AccessToken.getCurrentAccessToken().token else prefConfig.readToken()!!,
                userFields
            )
        } catch(ex: Exception) {
            try {
                Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
            } catch(authEx: AuthorisedRequestException) {
                getNewTokenWithRefresh()
                editUser(userFields) // recursive call to this again; if everything goes as planned, this should never cause a recursive loop
            }
        }
    }

    suspend fun deleteUser(): Response? {
        Log.i("TokenRepository", "deleteUser -> deleting current user")
        return try {
            hobbyfiAPI.deleteUser(
                if(Constants.isFacebookUserAuthd()) AccessToken.getCurrentAccessToken().token else prefConfig.readToken()!!
            )
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

    suspend fun saveUser(user: User) {
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_user_fetch_time)
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.userDao().insert(user)
        }
    }

    suspend fun deleteUser(user: User): Boolean {
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_user_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.withTransaction {
                val deletedUser = hobbyfiDatabase.userDao().delete(user)
                return@withTransaction deletedUser > 0 // auth user should not have remote keys stored
            }
        }
    }

    // called when user leaves chatroom
    suspend fun deleteUsers(authId: Long): Boolean { // pass in auth Id from cache user directly to avoid any expired token mishaps
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_chatroom_users_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.withTransaction {
                val deletedUsers = hobbyfiDatabase.userDao().deleteUsersExceptId(authId)
                val deletedRemoteKeys = hobbyfiDatabase.remoteKeysDao().deleteRemoteKeysExceptForIdAndForType(authId, RemoteKeyType.USER)
                return@withTransaction deletedUsers > 0 && deletedRemoteKeys >= 0
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
//            remoteMediator = remoteMediator
//        ).liveData
            throw Exception() // stub
        }
    }
}