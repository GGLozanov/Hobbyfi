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
    private val chatroomId: Long,
    private var messageId: Long? = null
): ModelMediator<Int, Message>(
    hobbyfiDatabase, prefConfig,
    hobbyfiAPI, RemoteKeyType.MESSAGE
) {
    private val messageDao = hobbyfiDatabase.messageDao()
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
                null
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

        val keys = mapRemoteKeysFromModelList(messagesResponse.modelList, page, isEndOfList)
        Log.i("MessageMediator", "MESSAGE RemoteKeys created. RemoteKeys: ${keys}")
        Log.i("MessageMediator", "Inserting ChatroomList and RemoteKeys")

        hobbyfiDatabase.withTransaction {
            val cacheTimeOut = Constants.cacheTimedOut(prefConfig, cachePrefId)
            // clear all rows in chatroom and remote keys table (for chatrooms)
            if (loadType == LoadType.REFRESH || cacheTimeOut) {
                Log.i("MessageMediator", "MESSAGE triggered refresh OR cache TIMEOUT. Clearing cache")
                clearCachedMessagesByFetchType()
            }
        }

        hobbyfiDatabase.withTransaction {
            remoteKeysDao.upsert(keys)
            messageDao.upsert(messagesResponse.modelList)
        }

        return MediatorResult.Success(endOfPaginationReached = isEndOfList)
    }

    private suspend fun clearCachedMessagesByFetchType() {
        remoteKeysDao.deleteMessagesRemoteKeysByChatroomId(chatroomId)
        messageDao.deleteMessagesByChatroomId(chatroomId)
    }

    override suspend fun getPage(loadType: LoadType, state: PagingState<Int, Message>): Any {
        return when(loadType) {
            LoadType.PREPEND -> {
                val remoteKeys = if(wasCalledWithSearchMessageIdPrior) {
                    Log.i("ModelRemoteM", "getKeyPageData => PREPEND Remote Keys TRIGGERED MAX CONDITION")
                    wasCalledWithSearchMessageIdPrior = false
                    remoteKeysDao.getMaxRemoteKeyByType(RemoteKeyType.MESSAGE)
                } else hobbyfiDatabase.withTransaction { getFirstRemoteKey(state) }

                Log.i("ModelRemoteM", "getKeyPageData => PREPEND Remote Keys: $remoteKeys")
                if(remoteKeys?.prevKey == null) {
                    Log.i("ModelRemoteM", "getKeyPageData => REMOTE MEDIATOR TRIGGERED RETURN (END OF PAGINATION) FOR PREPEND")

                   return MediatorResult.Success(endOfPaginationReached = true)
                }

                return remoteKeys.prevKey
            }
//            LoadType.APPEND -> {
//                val remoteKeys = hobbyfiDatabase.withTransaction {
//                    getLastRemoteKey(state)
//                }
//
//                if (remoteKeys?.nextKey == null) {
//                    Log.i(
//                        "ModelRemoteM",
//                        "getKeyPageData => REMOTE MEDIATOR TRIGGERED RETURN (END OF PAGINATION) FOR APPEND"
//                    )
//
//                    val minKeys = remoteKeysDao.getMinRemoteKeyByType(
//                        mainRemoteKeyType
//                    )
//                    Log.i("ModelRemoteM", "getKeyPageData => APPEND Remote Keys ON MIN CONDITION: $minKeys")
//
//                    return if(minKeys != null) {
//                        minKeys.nextKey ?: 2
//                    } else MediatorResult.Success(endOfPaginationReached = true)
//                }
//
//                remoteKeys.nextKey
//            }
            else -> super.getPage(loadType, state)
        }
    }
}