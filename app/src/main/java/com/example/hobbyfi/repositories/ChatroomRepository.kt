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
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.shared.Constants.getDefaultPageConfig
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class ChatroomRepository @ExperimentalPagingApi constructor(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager
) : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {

    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    suspend fun getChatrooms(
        pagingConfig: PagingConfig = getDefaultPageConfig(Constants.chatroomPageSize),
    ): Flow<PagingData<Chatroom>> {
        Log.i("ChatroomRepository", "Fetching normal chatrooms")
        return channelFlow {
            getUserChatroomIds(prefConfig.getAuthUserIdFromToken()).distinctUntilChanged().collectLatest {

                Log.i("ChatroomRepository", "getChatrooms -> Found current userIds with value: $it")
                val pagingSource = { if(it != null)
                        hobbyfiDatabase.chatroomDao().getChatroomsNotPresentInIds(it)
                    else hobbyfiDatabase.chatroomDao().getChatrooms()
                }

                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = pagingSource,
                    remoteMediator = ChatroomMediator(hobbyfiDatabase, prefConfig, hobbyfiAPI, false, it)
                ).flow.collectLatest { data ->
                    sendBlocking(data)
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    suspend fun getAuthChatrooms(
        pagingConfig: PagingConfig = getDefaultPageConfig(Constants.chatroomPageSize),
    ): Flow<PagingData<Chatroom>> {
        Log.i("ChatroomRepository", "Fetching Auth chatrooms")
        return channelFlow {
            getUserChatroomIds(prefConfig.getAuthUserIdFromToken()).distinctUntilChanged().collectLatest {
                if(it != null) {
                    Log.i("ChatroomRepository", "getAuthChatrooms -> Found current userIds with value: $it")
                    val pagingSource = { hobbyfiDatabase.chatroomDao().getChatroomsByIds(it) }

                    Pager(
                        config = pagingConfig,
                        pagingSourceFactory = pagingSource,
                        remoteMediator = ChatroomMediator(hobbyfiDatabase, prefConfig, hobbyfiAPI, true, it)
                    ).flow.collectLatest { data ->
                        sendBlocking(data)
                    }
                } else {
                    Log.i("ChatroomRepository", "getAuthChatrooms -> Invalid call to getAuthChatrooms without authchatroomids")
                }
            }
        }
    }

    // fetches chatroom when chatroomactivity is called from deeplink notification (for example)
    suspend fun getChatroom(): Flow<Chatroom?> {
        Log.i("ChatroomRepository", "getChatroom -> getting auth user chatroom")

        return object : NetworkBoundFetcher<Chatroom, CacheResponse<Chatroom>>() {
            override suspend fun saveNetworkResult(response: CacheResponse<Chatroom>) {
                hobbyfiDatabase.chatroomDao().upsert(response.model)
            }

            override fun shouldFetch(cache: Chatroom?): Boolean {
                // TODO: Check if it's wrong to reuse chatrooms last fetch time shared pref
                return adheresToDefaultCachePolicy(cache, R.string.pref_last_chatrooms_fetch_time)
            }

            override suspend fun loadFromDb(): Flow<Chatroom?> =
                hobbyfiDatabase.chatroomDao().getChatroomById(prefConfig.readLastEnteredChatroomId())

            override suspend fun fetchFromNetwork(): CacheResponse<Chatroom>? {
                Log.i("ChatroomRepository", "getChatroom -> fetchFromNetwork() -> fetching current auth chatroom from network")
                return performAuthorisedRequest({
                    val token = prefConfig.getAuthUserToken()
                    val response = hobbyfiAPI.fetchChatroom(
                        token!!,
                        prefConfig.readLastEnteredChatroomId()
                    )
                    Log.i("ChatroomRepository", "getChatroom -> ${response?.model}")
                    response
                }, { fetchFromNetwork() })
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
                if(tags.isEmpty()) null else Constants.tagJsonConverter.toJson(tags)
            )
        }, { createChatroom(name, description, base64Image, tags) })
    }

    suspend fun editChatroom(chatroomFields: Map<String, String?>): Response? {
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
            hobbyfiAPI.deleteChatroom(
                prefConfig.getAuthUserToken()!!,
            )
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


    private suspend fun getUserChatroomIds(userId: Long): Flow<List<Long>?> =
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.userDao().getUserChatroomIds(userId).map {
                Constants.tagJsonConverter.fromJson(it)
            }
        }
}