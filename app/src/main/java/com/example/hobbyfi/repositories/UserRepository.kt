package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.User
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// fetches both auth users & chatroom users
class UserRepository @ExperimentalPagingApi constructor(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI,
    hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager
): CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {

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
                // TODO: Edit a bit to use performAuthorisedRequest() method
                return try {
                    val userId = prefConfig.getAuthUserIdFromToken()

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
                return performAuthorisedRequest({
                    val response = hobbyfiAPI.fetchUser(
                        prefConfig.getAuthUserToken()!!,
                    )
                    Log.i("UserRepository", "getUser -> ${response?.model}")
                    response
                }, { fetchFromNetwork() })
            }
        }.asFlow()
    }

    suspend fun editUser(userFields: Map<String?, String?>): Response? {
        Log.i("TokenRepository", "editUser -> editing current user. Edit map: ${userFields}")
        return performAuthorisedRequest({
            hobbyfiAPI.editUser(
                prefConfig.getAuthUserToken()!!,
                userFields
            )
        }, {
            editUser(userFields) // recursive call to this again; if everything goes as planned, this should never cause a recursive loop
        })
    }

    suspend fun deleteUser(): Response? {
        Log.i("TokenRepository", "deleteUser -> deleting current user")
        return performAuthorisedRequest({
            hobbyfiAPI.deleteUser(
                prefConfig.getAuthUserToken()!!
            )
        }, {
            deleteUser()
        })
    }

    suspend fun saveUser(user: User, shouldWritePrefTime: Boolean = true) {
        if(shouldWritePrefTime) {
            prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_user_fetch_time)
        }

        withContext(Dispatchers.IO) {
            hobbyfiDatabase.userDao().upsert(user)
        }
    }

    suspend fun saveUsers(users: List<User>) {
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatroom_users_fetch_time)

        withContext(Dispatchers.IO) {
            hobbyfiDatabase.userDao().upsert(users)
        }
    }

    suspend fun deleteUserCache(userId: Long, shouldWritePrefTime: Boolean = true): Boolean {
        Log.i("UserRepository", "deleteUser -> deleting auth user w/ id: $userId")
        if(shouldWritePrefTime) {
            prefConfig.resetLastPrefFetchTime(R.string.pref_last_user_fetch_time)
        }
        
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.withTransaction {
                val deletedUser = hobbyfiDatabase.userDao().deleteUserById(userId)
                return@withTransaction deletedUser > 0 // auth user should not have remote keys stored
            }
        }
    }

    // called when user leaves chatroom (voluntarily or not - leave chatroom button or yeeted from chatroom)
    suspend fun deleteUsersCache(userId: Long): Boolean { // pass in auth Id from cache user directly to avoid any expired token mishaps
        Log.i("UserRepository", "deleteUsers -> deleting auth users with id: $userId")
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_chatroom_users_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.withTransaction {
                val deletedUsers = hobbyfiDatabase.userDao().deleteUsersExceptId(userId)
                return@withTransaction deletedUsers >= 0 // account for user alone in chatroom (rip); that's why >= 0
            }
        }
    }

    // return livedata of pagedlist for users
    suspend fun getChatroomUsers(chatroomId: Long):
            Flow<List<User>?> {
        Log.i("UserRepository", "getChatroomUsers -> getting current chatroom users")
        return object : NetworkBoundFetcher<List<User>, CacheListResponse<User>>() {
            override suspend fun saveNetworkResult(response: CacheListResponse<User>) {
                hobbyfiDatabase.withTransaction {
                    hobbyfiDatabase.userDao().deleteUsersByChatroomIdAndExceptId(
                        chatroomId,
                        prefConfig.getAuthUserIdFromToken()
                    )
                    saveUsers(response.modelList)
                }
            }

            override fun shouldFetch(cache: List<User>?): Boolean {
                Log.i("UserRepository", "getUserS => cache: $cache")
                Log.i("UserRepository", "getUserS => isConnected: " + connectivityManager.isConnected())
                Log.i("UserRepository", "getUserS => shouldFetch: " + (cache == null ||
                        Constants.cacheTimedOut(prefConfig, R.string.pref_last_chatroom_users_fetch_time) || connectivityManager.isConnected()))
                return adheresToDefaultCachePolicy(cache, R.string.pref_last_chatroom_users_fetch_time)
            }

            override suspend fun loadFromDb(): Flow<List<User>?> = hobbyfiDatabase.userDao().getUsersByChatroomId(chatroomId)

            override suspend fun fetchFromNetwork(): CacheListResponse<User>? = performAuthorisedRequest(
                {
                    val response = hobbyfiAPI.fetchUsers(
                        prefConfig.getAuthUserToken()!!,
                        chatroomId
                    )
                    Log.i("UserRepository", "getUserS -> ${response?.modelList}")
                    response
                }, { fetchFromNetwork() })
        }.asFlow()
    }
}