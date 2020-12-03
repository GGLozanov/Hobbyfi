package com.example.hobbyfi.paging.mediators

import android.net.ConnectivityManager
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.repositories.MessageRepository
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RemoteKeyType

@ExperimentalPagingApi
class MessageMediator(
    hobbyfiDatabase: HobbyfiDatabase,
    prefConfig: PrefConfig,
    private val hobbyfiAPI: HobbyfiAPI,
    private val connectivityManager: ConnectivityManager
) : ModelRemoteMediator<Int, Chatroom>(hobbyfiDatabase, prefConfig, RemoteKeyType.MESSAGE) {

    // TODO: get user token, get cached auth user from id, check his chatroom id and make request based on that
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Chatroom>
    ): MediatorResult {
        TODO("Not yet implemented")
        // insert new page numbers (remote keys) after using cached page number to fetch new one
        // if REFRESH LoadType => try to fetch new
    }
}