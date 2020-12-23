package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.paging.*
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.paging.mediators.MessageMediator
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

class MessageRepository @ExperimentalPagingApi constructor(
    private val remoteMediator: MessageMediator, prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI, hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
    // return livedata of pagedlist for messages
    @ExperimentalPagingApi
    fun getMessages(pagingConfig: PagingConfig = Constants.getDefaultPageConfig(Constants.messagesPageSize)):
            Flow<PagingData<Message>> {
        Log.i("ChatroomRepository", "getMessages -> getting current messages")
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { hobbyfiDatabase.messageDao().getMessages() }, // TODO: DI
            remoteMediator = remoteMediator
        ).flow
    }

    fun createMessage(message: String, userSentId: Int, chatroomSentId: Int) {

    }

    fun deleteMessage(id: Int) {

    }

    fun deleteMessageCache(id: Int) {

    }

    fun deleteMessagesCache(chatroomId: Int) {

    }

    fun editMessage(id: Int, messageFields: Map<String?, String?>) {

    }

    fun saveMessage(message: Message) {

    }
}