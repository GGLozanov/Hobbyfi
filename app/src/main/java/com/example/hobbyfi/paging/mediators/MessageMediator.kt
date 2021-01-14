package com.example.hobbyfi.paging.mediators

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType

@ExperimentalPagingApi
class MessageMediator(
    hobbyfiDatabase: HobbyfiDatabase,
    prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI,
    private val chatroomId: Long
) : ModelMediator<Int, Message>(hobbyfiDatabase, prefConfig, hobbyfiAPI, RemoteKeyType.MESSAGE) {
    private val messageDao = hobbyfiDatabase.messageDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Message>
    ): MediatorResult {
        // insert new page numbers (remote keys) after using cached page number to fetch new one
        // if REFRESH LoadType => try to fetch new

        return try {
            fetchMessages(state, loadType)
        } catch(ex: Exception) {
            ex.printStackTrace()
            try {
                Callbacks.dissectRepositoryExceptionAndThrow(ex)
            } catch(parsedEx: Exception) {
                MediatorResult.Error(parsedEx)
            }
            MediatorResult.Error(ex)
        }

    }

    private suspend fun fetchMessages(state: PagingState<Int, Message>, loadType: LoadType): MediatorResult {
        val page = getPage(loadType, state).let {
            when(it) {
                is MediatorResult.Success -> {
                    return@fetchMessages it
                }
                else -> {
                    it as Int
                }
            }
        }

        Log.i("MessageMediator", "Fetching next messages with page $page")

        val messagesResponse = hobbyfiAPI.fetchMessages(
            prefConfig.getAuthUserToken()!!,
            chatroomId,
            page,
        )

        val mediatorResult = saveMessages(messagesResponse, page, loadType)

        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatroom_messages_fetch_time)

        return mediatorResult
    }

    private suspend fun saveMessages(messagesResponse: CacheListResponse<Message>, page: Int, loadType: LoadType): MediatorResult {
        val isEndOfList = messagesResponse.modelList.isEmpty()
        Log.i("MessageMediator", "Fetched Messages.")
        Log.i("MessageMediator", "Reached end of list: ${isEndOfList}")
        Log.i("MessageMediator", "Messages list: ${messagesResponse.modelList}")

        hobbyfiDatabase.withTransaction {
            val cacheTimedOut = Constants.cacheTimedOut(prefConfig, R.string.pref_last_chatroom_messages_fetch_time)
            if (loadType == LoadType.REFRESH || cacheTimedOut) {
                Log.i("MessageMediator", "MESSAGE triggered refresh or timeout cache. Clearing cache. WasCacheTimedOut: ${cacheTimedOut}")
                remoteKeysDao.deleteRemoteKeysByTypeAndIds(mainRemoteKeyType, messageDao.getMessagesIdsByChatroomId(chatroomId))
                messageDao.deleteMessagesByChatroomId(chatroomId)
            }
            val keys = mapRemoteKeysFromModelList(messagesResponse.modelList, page, isEndOfList)
            Log.i("MessageMediator", "MESSAGE RemoteKeys created. RemoteKeys: ${keys}")
            Log.i("MessageMediator", "Inserting ChatroomList and RemoteKeys")
            remoteKeysDao.upsert(keys)
            messageDao.upsert(messagesResponse.modelList)
        }

        return MediatorResult.Success(endOfPaginationReached = isEndOfList)
    }
}