package com.example.hobbyfi.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.UserResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// fetches both auth users & chatroom users (map<string, user>)
class UserRepository(
    pagingSource: PagingSource<Int, User>, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase) {

    suspend fun getUser(): Flow<User?> {
        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")
            return@withContext object : NetworkBoundFetcher<User, UserResponse>() {
                override suspend fun saveNetworkResult(item: UserResponse) {
                    TODO("Not yet implemented")
                }

                override fun shouldFetch(data: User?): Boolean {
                    TODO("Not yet implemented")
                }

                override fun loadFromDb(): Flow<User> {
                    TODO("Not yet implemented")
                }

                override suspend fun fetchFromNetwork(): UserResponse {
                    TODO("Not yet implemented")
                }

                override fun isNetworkResponseInvalid(response: UserResponse): Boolean {
                    TODO("Not yet implemented")
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
                    null
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
                    null
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
}