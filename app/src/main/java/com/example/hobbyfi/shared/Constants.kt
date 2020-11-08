package com.example.hobbyfi.shared

import androidx.paging.PagingConfig
import com.example.hobbyfi.models.Tag

object Constants {
    // TODO: Put in-memory tags here
    val predefinedTags: List<Tag> = listOf()

    fun getDefaultPageConfig(): PagingConfig { // used in pager init
        return PagingConfig(pageSize = 5, enablePlaceholders = false)
    }
}