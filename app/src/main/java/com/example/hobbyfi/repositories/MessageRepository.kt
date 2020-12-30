package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.paging.*
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.paging.mediators.MessageMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CreateTimeIdResponse
import com.example.hobbyfi.responses.IdResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MessageRepository @ExperimentalPagingApi constructor(
    private val remoteMediator: MessageMediator, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
    @ExperimentalPagingApi
    fun getMessages(pagingConfig: PagingConfig = Constants.getDefaultPageConfig(Constants.messagesPageSize)):
            Flow<PagingData<Message>> {
        Log.i("MessageRepository", "getMessages -> getting current messages")
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { hobbyfiDatabase.messageDao().getMessages() }, // TODO: DI
            remoteMediator = remoteMediator
        ).flow
    }

    suspend fun createMessage(message: String): CreateTimeIdResponse? {
        Log.i("MessageRepository", "createMessage -> Creating new message with auth user id")
        return performAuthorisedRequest({
            hobbyfiAPI.createMessage(
                prefConfig.getAuthUserToken()!!,
                message
            )
        }, { createMessage(message) })
    }

    suspend fun deleteMessage(id: Long): Response? {
        Log.i("MessageRepository", "deleteMessage -> Deleting auth user (or chatroom owner) message with id $id")
        return performAuthorisedRequest({
            hobbyfiAPI.deleteMessage(
                prefConfig.getAuthUserToken()!!,
                id
            )
        }, { deleteMessage(id) })
    }

    suspend fun deleteMessageCache(id: Long): Boolean {
        Log.i("MessageRepository", "deleteMessageCache -> deleting cached message w/ id: $id")
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.messageDao().deleteMessageById(id) > 0
        }
    }

    suspend fun deleteMessagesCache(): Boolean {
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_chatroom_messages_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.messageDao().deleteMessages() > 0
        }
    }

    suspend fun editMessage(messageFields: Map<String?, String?>): Response? {
        Log.i("MessageRepository", "editMessage -> Editing auth user message with " +
                "id ${messageFields[Constants.ID] ?: error("Message edit ID must not be null")} " +
                "and mesage fields: $messageFields")
        return performAuthorisedRequest({
            hobbyfiAPI.editMessage(
                prefConfig.getAuthUserToken()!!,
                messageFields
            )
        }, { editMessage(messageFields) })
    }

    suspend fun saveNewMessage(message: Message) =
        withContext(Dispatchers.IO) {
            // set to the beginning of the page remote key and wake SQL page shift trigger
            hobbyfiDatabase.remoteKeysDao().insert(RemoteKeys(message.id, 2, null, RemoteKeyType.MESSAGE))
            hobbyfiDatabase.messageDao().insert(message)
        }

    // message is the only mutable property (FOR NOW)
    suspend fun updateMessageCache(id: Long, message: String) =
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.messageDao().updateMessageById(id, message)
        }

    suspend fun saveMessages(messages: List<Message>) {
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatroom_messages_fetch_time)
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.messageDao().insertList(messages)
        }
    }
}