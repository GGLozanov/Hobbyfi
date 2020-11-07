package com.example.hobbyfi.paging.sources

import androidx.paging.PagingSource
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Chatroom

// API is injected from repository where Pager is initialised with this data source
class ChatroomPagingSource(
    private val hobbyfiAPI: HobbyfiAPI
) : PagingSource<Int, Chatroom>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Chatroom> {
        TODO("Not yet implemented")
        // handle NoConnectivityException and load from cache
    }
}