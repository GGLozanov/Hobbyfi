package com.example.hobbyfi.paging.sources

import androidx.paging.PagingSource
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Message

// API is injected from repository where Pager is initialised with this data source
// TODO: Register in Kodein instance used in repository & viewmodel
// TODO: Swipe to refresh listeners trigger viewmodel method to access Kodein PagingSource instance
// TODO: and trigger invalidate() to refresh data from network
// TODO: This is retriggered and should (hopefully) retrigger repository method somehow (?)
class MessagePagingSource(
    private val chatroomId: Int,
    private val hobbyfiAPI: HobbyfiAPI,
) : PagingSource<Int, Message>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
        TODO("Not yet implemented")
            // handle NoConnectivityException and load from cache
    }
}