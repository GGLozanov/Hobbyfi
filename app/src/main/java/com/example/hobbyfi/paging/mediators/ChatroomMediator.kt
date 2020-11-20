package com.example.hobbyfi.paging.mediators

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.repositories.ChatroomRepository

@ExperimentalPagingApi
class ChatroomMediator(
    private val chatroomRepository: ChatroomRepository
) : RemoteMediator<Int, Chatroom>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Chatroom>
    ): MediatorResult {
        TODO("Not yet implemented")
        // insert new page numbers (remote keys) after using cached page number to fetch new one
    }
}