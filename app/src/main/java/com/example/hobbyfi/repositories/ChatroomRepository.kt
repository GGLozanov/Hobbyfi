package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.paging.mediators.ChatroomMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.IdResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.Constants.getDefaultPageConfig
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType
import com.google.firebase.FirebaseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatroomRepository @ExperimentalPagingApi constructor(
    private val chatroomMediator: ChatroomMediator, private val authChatroomMediator: ChatroomMediator,
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager
) : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
    @ExperimentalPagingApi
    fun getChatrooms(
        pagingConfig: PagingConfig = getDefaultPageConfig(Constants.chatroomPageSize),
        userChatroomIds: List<Long>?
    ): Flow<PagingData<Chatroom>> {
        val pagingSource = { hobbyfiDatabase.chatroomDao().getChatroomsNotPresentInIds(userChatroomIds) }
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = pagingSource,
            remoteMediator = chatroomMediator
        ).flow
    }

    @ExperimentalPagingApi
    fun getAuthChatrooms(
        pagingConfig: PagingConfig = getDefaultPageConfig(Constants.chatroomPageSize),
        userChatroomIds: List<Long>?
    ): Flow<PagingData<Chatroom>> {
        val pagingSource = { hobbyfiDatabase.chatroomDao().getChatroomsByIds(userChatroomIds) }
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = pagingSource,
            remoteMediator = authChatroomMediator
        ).flow
    }

    // fetches chatroom when chatroomactivity is called from deeplink notification (for example)
    suspend fun getChatroom(): Flow<Chatroom?> {
        Log.i("ChatroomRepository", "getChatroom -> getting auth user chatroom")

        return object : NetworkBoundFetcher<Chatroom, CacheResponse<Chatroom>>() {
            override suspend fun saveNetworkResult(response: CacheResponse<Chatroom>) {
                prefConfig.writeLastEnteredChatroomId(response.model.id)
                hobbyfiDatabase.chatroomDao().upsert(response.model)
            }

            override fun shouldFetch(cache: Chatroom?): Boolean {
                // TODO: Check if it's wrong to reuse chatrooms last fetch time shared pref
                return adheresToDefaultCachePolicy(cache, R.string.pref_last_chatrooms_fetch_time)
            }

            override suspend fun loadFromDb(): Flow<Chatroom?> {
                Log.i("ChatroomRepository", "getChatroom -> ${prefConfig.readToken()}")
                return try {
                    val ownerId = prefConfig.getAuthUserIdFromToken()

                    hobbyfiDatabase.chatroomDao().getChatroomByOwnerId(ownerId)
                } catch(ex: Exception) {
                    try {
                        Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
                    } catch(authEx: AuthorisedRequestException) {
                        getNewTokenWithRefresh()
                        loadFromDb()
                    }
                }
            }

            override suspend fun fetchFromNetwork(): CacheResponse<Chatroom>? {
                Log.i("ChatroomRepository", "getChatroom -> fetchFromNetwork() -> fetching current auth chatroom from network")
                return try {
                    val token = prefConfig.getAuthUserToken()
                    if(token != null) {
                        val response = hobbyfiAPI.fetchChatroom(
                            token,
                            prefConfig.readLastEnteredChatroomId()
                        )
                        Log.i("ChatroomRepository", "getChatroom -> ${response.model}")
                        response
                    } else throw ReauthenticationException()
                } catch(ex: Exception) {
                    ex.printStackTrace()
                    Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true) // no need for authorised request handling here because token parsing already handles expired token exceptions
                }
            }
        }.asFlow()
    }

    suspend fun createChatroom(name: String, description: String?,
                               base64Image: String?, tags: List<Tag>): IdResponse? {
        return performAuthorisedRequest({
            Log.i("ChatroomRepository", "createChatroom -> creating chatroom with name:"
                    + name + "; description:" + description + "; ownerId: " + prefConfig.getAuthUserIdFromToken() + "; image: " + base64Image + "; tags: " + tags)

            hobbyfiAPI.createChatroom(
                prefConfig.getAuthUserToken()!!,
                name,
                description,
                base64Image,
                if(tags.isEmpty()) null else tags
            )
        }, { createChatroom(name, description, base64Image, tags) })
    }

    suspend fun editChatroom(chatroomFields: Map<String?, String?>): Response? {
        Log.i("TokenRepository", "editChatroom -> editing current chatroom")

        return performAuthorisedRequest({
            hobbyfiAPI.editChatroom(
                prefConfig.getAuthUserToken()!!,
                chatroomFields
            )
        }, { editChatroom(chatroomFields) })
    }

    suspend fun deleteChatroom(chatroomId: Long): Response? {
        Log.i("ChatroomRepository", "deleteChatroom -> deleting current chatroom")

        return performAuthorisedRequest({
            val response = hobbyfiAPI.deleteChatroom(
                prefConfig.getAuthUserToken()!!,
            )

            firestore.collection(Constants.LOCATIONS_COLLECTION)
                .whereEqualTo(Constants.CHATROOM_ID, chatroomId)
                .get().addOnSuccessListener {
                    it.documents.forEach { doc ->
                        doc.reference.delete()
                    }
                }.addOnFailureListener {
                    throw FirebaseException(Constants.firestoreDeletionError)
                }
            response
        }, { deleteChatroom(chatroomId) })
    }

    suspend fun saveChatroom(chatroom: Chatroom) {
        Log.i("ChatroomRepository", "editChatroomCache -> editing auth chatroom with owner id: ${chatroom.ownerId} and id: ${chatroom.id}")
        Log.i("ChatroomRepository", "editChatroomCache -> NEW CHATROOM: ${chatroom}")

        prefConfig.resetLastPrefFetchTime(R.string.pref_last_chatrooms_fetch_time)
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.chatroomDao().upsert(chatroom)
        }
    }

    suspend fun deleteChatroomCache(chatroom: Chatroom): Boolean {
        Log.i("ChatroomRepository", "deleteChatroomCache -> deleting auth chatroom with owner id: ${chatroom.ownerId} and id: ${chatroom.id}")
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_chatrooms_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.withTransaction {
                val deletedChatroom = hobbyfiDatabase.chatroomDao().delete(chatroom)
                val deletedRemoteKeys = hobbyfiDatabase.remoteKeysDao().deleteRemoteKeysForIdAndType(chatroom.id, RemoteKeyType.CHATROOM)
                deletedChatroom > 0 && deletedRemoteKeys >= 0
            }
        }
    }

    // called when user joins their chatroom for the first time after recyclerview
    suspend fun deleteChatrooms(authChatroomId: Long): Boolean {
        Log.i("ChatroomRepository", "deleteChatrooms -> deleting all chatrooms except for one with id: $authChatroomId")
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_chatrooms_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.withTransaction {
                val deletedChatrooms = hobbyfiDatabase.chatroomDao().deleteChatroomsExceptId(authChatroomId)
                val deletedRemoteKeys = hobbyfiDatabase.remoteKeysDao().deleteRemoteKeysExceptForIdAndForType(authChatroomId, RemoteKeyType.CHATROOM)
                Log.i("ChatroomRepository", "Deleted chatrooms ${deletedChatrooms} and deleted RMKeys ${deletedRemoteKeys}")
                deletedChatrooms > 0 && deletedRemoteKeys >= 0
            }
        }
    }
}