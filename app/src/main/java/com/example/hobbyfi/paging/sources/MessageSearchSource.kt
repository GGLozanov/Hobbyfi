package com.example.hobbyfi.paging.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.shared.PrefConfig

class MessageSearchSource(
    private val prefConfig: PrefConfig,
    private val hobbyfiAPI: HobbyfiAPI,
    private val chatroomId: Long,
    private val query: String
): PagingSource<Int, Message>() {
    override fun getRefreshKey(state: PagingState<Int, Message>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> =
        try {
            // Start refresh at page 1 if undefined.
            val nextPage = params.key ?: 1
            val response = hobbyfiAPI.fetchMessages(
                prefConfig.getAuthUserToken()!!,
                chatroomId,
                nextPage,
                query
            )

            LoadResult.Page(
                response.modelList, prevKey = if (nextPage == DEFAULT_PAGE_INDEX) null else nextPage - 1,
                nextKey = if (response.modelList.isEmpty()) null else nextPage + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    companion object {
        const val DEFAULT_PAGE_INDEX = 1
    }
}