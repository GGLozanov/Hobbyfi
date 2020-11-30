package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.paging.*
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.User
import com.example.hobbyfi.paging.mediators.UserMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.isConnected
import com.example.hobbyfi.utils.TokenUtils
import com.facebook.AccessToken
import com.facebook.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

// fetches both auth users & chatroom users (map<string, user>)
class UserRepository @ExperimentalPagingApi constructor(
    private val remoteMediator: UserMediator, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {

    // TODO: Fix this method w/ boolean. Becomes waaaaaaaay too inflexible and coupled
    suspend fun getUser(isFacebookUser: Boolean): Flow<User?> {
        return withContext(Dispatchers.IO) {
            Log.i("UserRepository", "getUser -> getting current user")
            return@withContext object : NetworkBoundFetcher<User, CacheResponse<User>>() {
                override suspend fun saveNetworkResult(response: CacheResponse<User>) {
                    saveUser(response.model)
                }

                override fun shouldFetch(cache: User?): Boolean {
                    val lastUserFetchTime = prefConfig.readLastUserFetchTime()
                    Log.i("UserRepository", "getUser => isConnected: " + connectivityManager.isConnected())
                    Log.i("UserRepository", "getUser => shouldFetch: " + (cache == null ||
                            ((System.currentTimeMillis() / 1000) - lastUserFetchTime) <= Constants.CACHE_TIMEOUT || connectivityManager.isConnected()))
                    return cache == null ||
                            ((System.currentTimeMillis() / 1000) - lastUserFetchTime) <= Constants.CACHE_TIMEOUT || connectivityManager.isConnected()
                }

                override suspend fun loadFromDb(): Flow<User?> {
                    Log.i("UserRepository", "getUser -> ${prefConfig.readToken()}")
                    return try {
                        val userId = getUserIdFromToken(isFacebookUser)

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
                        val token = if(isFacebookUser) AccessToken.getCurrentAccessToken().token else prefConfig.readToken()
                        if(token != null) {
                            val response = hobbyfiAPI.fetchUser(token)
                            Log.i("UserRepository", "getUser -> ${response?.model}")
                            response
                        } else null
                    } catch(ex: Exception) {
                        Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true) // no need for authorised request handling here because token parsing already handles expired token exceptions
                    }
                }
            }.asFlow()
        }
    }

    suspend fun editUser(isFacebookUser: Boolean, userFields: Map<String?, String?>): Response? {
        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")
            return@withContext try {
                val userId = getUserIdFromToken(isFacebookUser) // validate token expiry by attempting to get id
                hobbyfiAPI.editUser(
                    if(isFacebookUser) AccessToken.getCurrentAccessToken().token else prefConfig.readToken()!!,
                    userFields
                )
            } catch(ex: Exception) {
                try {
                    Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
                } catch(authEx: AuthorisedRequestException) {
                    getNewTokenWithRefresh()
                    editUser(isFacebookUser, userFields) // recursive call to this again; if everything goes as planned, this should never cause a recursive loop
                }
            }
        }
    }

    suspend fun deleteUser(isFacebookUser: Boolean): Response? {
        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")
            return@withContext try {
                hobbyfiAPI.deleteUser(
                    if(isFacebookUser) AccessToken.getCurrentAccessToken().token else prefConfig.readToken()!!
                ) // TODO: Error handle for token value = "invalid"?
            } catch(ex: Exception) {
                try {
                    Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
                } catch(authEx: AuthorisedRequestException) {
                    val token = getNewTokenWithRefresh()
                        // if this ^ throws exception => user reauth; invalid refresh token & can't fetch response
                    deleteUser(isFacebookUser)
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

    suspend fun saveUser(user: User) {
        prefConfig.writeLastUserFetchTimeNow()
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.userDao().insert(user)
        }
    }

    private fun getUserIdFromToken(isFacebookUser: Boolean): Long {
        if(isFacebookUser) {
            if(AccessToken.getCurrentAccessToken() == null && AccessToken.getCurrentAccessToken().isExpired) {
                throw ReauthenticationException("Your Facebook token has expired! Please login again!")
                    // Callbacks.dissectRepositoryExceptionAndThrow() immediately recognises this and throws it again
            }

            return Profile.getCurrentProfile().id.toLong()
        }

        return TokenUtils.getTokenUserIdFromPayload(prefConfig.readToken())
    }

}