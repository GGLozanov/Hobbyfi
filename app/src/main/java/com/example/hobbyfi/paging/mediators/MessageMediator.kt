package com.example.hobbyfi.paging.mediators

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.room.withTransaction
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.suspendCoroutine

@ExperimentalPagingApi
class MessageMediator(
    hobbyfiDatabase: HobbyfiDatabase,
    prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI,
    private val chatroomId: Long,
    private val query: String? = null,
    private var messageId: Long? = null
): ModelMediator<Int, Message>(
    hobbyfiDatabase, prefConfig,
    hobbyfiAPI, RemoteKeyType.MESSAGE
) {
    private val messageDao = hobbyfiDatabase.messageDao()
    private val searchMessages: Boolean get() = query != null
    private val searchMessageId: Boolean get() = messageId != null

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Message>
    ): MediatorResult {
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
        var page = getPage(loadType, state).let {
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

        val messagesResponse = if(!searchMessageId) {
            hobbyfiAPI.fetchMessages(
                prefConfig.getAuthUserToken()!!,
                chatroomId,
                page,
                query
            )
        } else {
            val messagesPageResponse = hobbyfiAPI.fetchMessagesId(
                prefConfig.getAuthUserToken()!!, chatroomId, messageId!!
            )
            page = messagesPageResponse.page
            CacheListResponse(messagesPageResponse.response, messagesPageResponse.modelList)
        }

        val mediatorResult = saveMessages(messagesResponse, page, loadType)

        if(searchMessageId) {
            messageId = null // reset message search and opt for normal page loading
        }

        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_chatroom_messages_fetch_time)

        return mediatorResult
    }

    private suspend fun saveMessages(messagesResponse: CacheListResponse<Message>, page: Int, loadType: LoadType): MediatorResult {
        val isEndOfList = messagesResponse.modelList.isEmpty()
        Log.i("MessageMediator", "Fetched Messages.")
        Log.i("MessageMediator", "Reached end of list: ${isEndOfList}")
        Log.i("MessageMediator", "Messages list: ${messagesResponse.modelList}")

        hobbyfiDatabase.withTransaction {
            if(!searchMessages) {
                remoteKeysDao.deleteRemoteKeysByTypeAndIds(RemoteKeyType.SEARCH_MESSAGE, messagesResponse.modelList.map { it.id })
            }

            val keys = mapRemoteKeysFromModelList(messagesResponse.modelList, page, isEndOfList)
            Log.i("MessageMediator", "MESSAGE RemoteKeys created. RemoteKeys: ${keys}")
            Log.i("MessageMediator", "Inserting ChatroomList and RemoteKeys")
            val cacheTimedOut = Constants.cacheTimedOut(prefConfig, R.string.pref_last_chatroom_messages_fetch_time)

            if (loadType == LoadType.REFRESH || cacheTimedOut) {
                Log.i("MessageMediator", "MESSAGE triggered refresh or timeout cache. Clearing cache. WasCacheTimedOut: ${cacheTimedOut}")
                clearCachedMessagesByFetchType()
            }

            remoteKeysDao.upsert(keys)
            messageDao.upsert(messagesResponse.modelList)
        }

        return MediatorResult.Success(endOfPaginationReached = isEndOfList)
    }

    override suspend fun getRemoteKeysByIdAndType(modelId: Long): RemoteKeys? =
        remoteKeysDao.getRemoteKeysByIdAndType(modelId, if(searchMessages) RemoteKeyType.SEARCH_MESSAGE else mainRemoteKeyType)

    private suspend fun clearCachedMessagesByFetchType() {
        if(searchMessages) {
            val deletedMessagesIds = messageDao.getMessagesIdsByChatroomIdAndIds(
                chatroomId, remoteKeysDao.getRemoteKeysIdsByType(RemoteKeyType.SEARCH_MESSAGE))
            remoteKeysDao.deleteRemoteKeysByTypeAndIds(RemoteKeyType.SEARCH_MESSAGE, messageDao.getMessagesIdsByChatroomId(chatroomId))
            messageDao.deleteMessagesByIds(deletedMessagesIds)
        } else {
            remoteKeysDao.deleteRemoteKeysByTypeAndIds(mainRemoteKeyType, messageDao.getMessagesIdsByChatroomId(chatroomId))
            messageDao.deleteMessagesByChatroomId(chatroomId)
        }
    }

    override fun mapRemoteKeysFromModelList(
        modelList: List<Message>,
        page: Int,
        isEndOfList: Boolean
    ): List<RemoteKeys> {
        val prevKey = if (page == DEFAULT_PAGE_INDEX) null else page - 1
        val nextKey = if (isEndOfList) null else page + 1

        Log.i("MessageMediator", "RemoteKeys calculated. Previous page: ${prevKey}; Next page: ${nextKey}")
        return modelList.map {
            RemoteKeys(id = it.id, prevKey = prevKey, nextKey = nextKey,
                modelType = if(searchMessages) RemoteKeyType.SEARCH_MESSAGE else mainRemoteKeyType
            )
        }
    }
}