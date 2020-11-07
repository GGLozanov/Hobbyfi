package com.example.hobbyfi.paging.sources

import androidx.paging.PagingSource
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.User

// API is injected from repository where Pager is initialised with this data source
// no need to generify these classes due to each overriding being different
class UserPagingSource(
    private val chatroomId: Int,
    private val hobbyfiAPI: HobbyfiAPI
) : PagingSource<Int, User>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, User> {
        TODO("Not yet implemented")
    }
}