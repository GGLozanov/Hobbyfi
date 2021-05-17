package com.example.hobbyfi.responses

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CacheListPageResponse<out T>(
    val response: String?,
    @SerializedName("data_list")
    @Expose
    val modelList: List<T>,
    @Expose
    val page: Int
)
