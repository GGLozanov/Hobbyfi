package com.example.hobbyfi.paging.mediators

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.shared.PrefConfig
import com.google.firebase.firestore.auth.User

@ExperimentalPagingApi
class UserMediator(
    private val hobbyfiAPI: HobbyfiAPI,
    private val hobbyfiDatabase: HobbyfiDatabase,
    private val prefConfig: PrefConfig
) : RemoteMediator<Int, User>() {

    // TODO: get user token, get cached auth user from id, check his chatroom id and make request based on that
    override suspend fun load(loadType: LoadType, state: PagingState<Int, User>): MediatorResult {
        TODO("Not yet implemented")
        // get remote keys & try network request & check for NoConnectivityException; if present, just return no success mediator result
        // and fetch whatever you can from old cache
        // if refresh request, check connectivity first and then delete db (don't accidentally delete old cache when user is offline!)
        // insert new page numbers (remote keys) after using cached page number to fetch new one
    }
}