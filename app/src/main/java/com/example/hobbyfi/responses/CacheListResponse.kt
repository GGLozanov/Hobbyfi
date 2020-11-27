package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Model

data class CacheListResponse<out T: Model>(
    val response: String?,
    val modelList: List<T>
)
