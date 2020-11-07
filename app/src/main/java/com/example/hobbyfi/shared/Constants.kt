package com.example.hobbyfi.shared

import androidx.paging.PagingConfig

object Constants {
    // TODO: Put in-memory tags here

    fun getDefaultPageConfig(): PagingConfig { // used in pager init
        return PagingConfig(pageSize = 5, enablePlaceholders = false)
    }
}