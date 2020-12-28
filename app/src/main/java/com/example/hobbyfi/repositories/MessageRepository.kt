package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.paging.*
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.paging.mediators.MessageMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CreateTimeIdResponse
import com.example.hobbyfi.responses.IdResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
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

    suspend fun deleteMessage(id: Int): Response? {
        Log.i("MessageRepository", "deleteMessage -> Deleting auth user (or chatroom owner) message with id $id")
        return performAuthorisedRequest({
            hobbyfiAPI.deleteMessage(
                prefConfig.getAuthUserToken()!!,
                id
            )
        }, { deleteMessage(id) })
    }

    suspend fun deleteMessageCache(id: Int) {
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

    suspend fun saveMessage(message: Message) =
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.messageDao().insert(message)
        }

    suspend fun saveMessages(messages: List<Message>) {
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatroom_messages_fetch_time)
        withContext(Dispatchers.IO) {
            hobbyfiDatabase.messageDao().insertList(messages)
        }
    }

    suspend fun calculateLatestRemoteKeys() {
        // TODO: Calculate latest remote key here by inserting
        // TODO: nextKey == null (always)
        // TODO: Somehow account for page shifting with remote key insertion and make all pages change
        // TODO: Ex: 21 messages for 1 remotekey pair => shift it to next page => shift all pages if exceeds page capacity (20)
    }
}