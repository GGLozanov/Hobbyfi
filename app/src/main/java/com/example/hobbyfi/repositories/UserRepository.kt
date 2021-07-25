package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.utils.TokenUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

// fetches both auth users & chatroom users
class UserRepository @ExperimentalPagingApi constructor(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI,
    hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
): CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager, coroutineDispatcher) {

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
                        dissectExceptionAndThrow(ex, isAuthorisedRequest = true)
                    } catch(tokenEx: TokenUtils.InvalidStoredTokenException) {
                        Log.w("UserRepository", "getUser() -> getNewTokenWithRefresh returned InvalidStoredTokenException")
                        flowOf(null)
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

    suspend fun editUser(userFields: Map<String, String?>, originalUsername: String): Response? {
        Log.i("TokenRepository", "editUser -> editing current user. Edit map: ${userFields}")
        return performAuthorisedRequest({
            hobbyfiAPI.editUser(
                prefConfig.getAuthUserToken()!!,
                userFields.filterKeys { it != Constants.IMAGE }
            )
        }, {
            editUser(userFields, originalUsername) // recursive call to this again; if everything goes as planned, this should never cause a recursive loop
        })
    }

    suspend fun deleteUser(username: String): Response? {
        Log.i("TokenRepository", "deleteUser -> deleting current user")
        return performAuthorisedRequest({
            hobbyfiAPI.deleteUser(
                prefConfig.getAuthUserToken()!!
            )
        }, {
            deleteUser(username)
        })
    }

    suspend fun saveUser(user: User, shouldWritePrefTime: Boolean = true) {
        if(shouldWritePrefTime) {
            prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_user_fetch_time)
        }

        withContext(coroutineDispatcher) {
            hobbyfiDatabase.userDao().upsert(user)
        }
    }

    suspend fun saveUsers(users: List<User>) {
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatroom_users_fetch_time)

        withContext(coroutineDispatcher) {
            hobbyfiDatabase.userDao().upsert(users)
        }
    }

    suspend fun setUserPhotoUrl(userId: Long, photoUrl: String, shouldWritePrefTime: Boolean = true) {
        if(shouldWritePrefTime) {
            prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_user_fetch_time)
        }

        withContext(coroutineDispatcher) {
            hobbyfiDatabase.userDao().updateUserPhotoUrl(userId, photoUrl)
        }
    }

    suspend fun deleteUserCache(userId: Long, shouldWritePrefTime: Boolean = true): Boolean {
        Log.i("UserRepository", "deleteUser -> deleting auth user w/ id: $userId")
        if(shouldWritePrefTime) {
            prefConfig.resetLastPrefFetchTime(R.string.pref_last_user_fetch_time)
        }
        
        return withContext(coroutineDispatcher) {
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
        return withContext(coroutineDispatcher) {
            hobbyfiDatabase.withTransaction {
                val deletedUsers = hobbyfiDatabase.userDao().deleteUsersExceptId(userId)
                return@withTransaction deletedUsers >= 0 // account for user alone in chatroom (rip); that's why >= 0
            }
        }
    }

    // need to map/filer individually in this method and not in queries
    // because filtering by json-encoded chatroom id list probably won't work
    @ExperimentalCoroutinesApi
    suspend fun getChatroomUsers(chatroomId: Long, authId: Long):
            Flow<List<User>?> {
        Log.i("UserRepository", "getChatroomUsers -> getting current chatroom users")
        return object : NetworkBoundFetcher<List<User>, CacheListResponse<User>>() {
            override suspend fun saveNetworkResult(response: CacheListResponse<User>) {
                hobbyfiDatabase.withTransaction {
                    with(hobbyfiDatabase.userDao()) {
                        getUsersImmediateExceptId(authId)?.filter {
                            it.chatroomIds?.contains(chatroomId) == true
                        }.let {
                            if(it != null) {
                                delete(
                                    it,
                                )
                            } else {
                                Log.w("UserRepository", "getChatroomUsers -> Filtered users from DB in saveNetworkResult call are null! " +
                                        "Not deleting any users initially from chatroom!")
                            }

                            saveUsers(response.modelList)
                        }
                    }
                }
            }

            override fun shouldFetch(cache: List<User>?): Boolean {
                Log.i("UserRepository", "getUserS => cache: $cache")
                Log.i("UserRepository", "getUserS => isConnected: " + connectivityManager.isConnected())
                Log.i("UserRepository", "getUserS => shouldFetch: " + (cache == null ||
                        Constants.cacheTimedOut(prefConfig, R.string.pref_last_chatroom_users_fetch_time) || connectivityManager.isConnected()))
                return adheresToDefaultCachePolicy(cache, R.string.pref_last_chatroom_users_fetch_time)
            }

            override suspend fun loadFromDb(): Flow<List<User>?> =
                hobbyfiDatabase.userDao().getUsers().mapLatest {
                    it?.filter { user -> user.chatroomIds?.contains(chatroomId) == true } } // bruh but SQL's giving issues with filtering in lists

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

    suspend fun togglePushNotificationAllowForChatroomUser(chatroomId: Long, allow: Boolean) {
        Log.i("UserRepository", "togglePushNotificationAllowForChatroomUser -> toggling push notifictation for auth user and chatroom w/ id $chatroomId with toggle status: $allow")
        return performAuthorisedRequest({
            hobbyfiAPI.togglePushNotificationAllowForChatroom(
                prefConfig.getAuthUserToken()!!,
                chatroomId,
                if(allow) 1 else 0
            )
        }, {
            togglePushNotificationAllowForChatroomUser(chatroomId, allow)
        })
    }
}