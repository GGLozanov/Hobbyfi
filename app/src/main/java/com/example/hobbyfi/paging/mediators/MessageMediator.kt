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
    private var wasCalledWithSearchMessageIdPrior = false

    override val cachePrefId: Int
        get() = R.string.pref_last_chatroom_messages_fetch_time

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
            wasCalledWithSearchMessageIdPrior = true
            messageId = null // reset message search and opt for normal page loading
        }

        prefConfig.writeLastPrefFetchTimeNow(cachePrefId)

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

            val cacheTimeOut = Constants.cacheTimedOut(prefConfig, cachePrefId)
            // clear all rows in chatroom and remote keys table (for chatrooms)
            if (loadType == LoadType.REFRESH || cacheTimeOut) {
                Log.i("ChatroomMediator", "CHATROOM triggered refresh OR cache TIMEOUT. Clearing cache")
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
            remoteKeysDao.deleteRemoteKeysByIds(
                messageDao.getMessagesIdsByChatroomIdAndRemoteKeyTypeInner(chatroomId, RemoteKeyType.SEARCH_MESSAGE)
            )
            messageDao.deleteMessagesByIds(deletedMessagesIds)
        } else {
            remoteKeysDao.deleteRemoteKeysByTypeAndChatroomId(mainRemoteKeyType, chatroomId)
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

    override suspend fun getPage(loadType: LoadType, state: PagingState<Int, Message>): Any {
        return when(loadType) {
            LoadType.PREPEND -> {
                // load type for whenever data needs to be prepended to the paged list (scroll up after down)
                val remoteKeys = hobbyfiDatabase.withTransaction {
                    getFirstRemoteKey(state)
                }
                // end of list condition reached -> reached the top of the page where the first page is loaded initially
                // which means we can set endOfPaginationReached to true
                Log.i("ModelRemoteM", "getKeyPageData => PREPEND Remote Keys: $remoteKeys")
                if(remoteKeys?.prevKey == null) {
                    Log.i("ModelRemoteM", "getKeyPageData => REMOTE MEDIATOR TRIGGERED RETURN (END OF PAGINATION) FOR PREPEND")

                    return if(wasCalledWithSearchMessageIdPrior) {
                        wasCalledWithSearchMessageIdPrior = false
                        val remoteKeysNew = remoteKeysDao.getMaxRemoteKeyByType(RemoteKeyType.MESSAGE)
                        Log.i("ModelRemoteM", "getKeyPageData => PREPEND Remote Keys ON MAX CONDITION: $remoteKeysNew")
                        remoteKeysNew!!.prevKey!!
                    } else MediatorResult.Success(endOfPaginationReached = true)
                }

                remoteKeys.prevKey
            }
            else -> super.getPage(loadType, state)
        }
    }
}