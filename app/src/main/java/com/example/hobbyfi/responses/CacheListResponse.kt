package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Model
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CacheListResponse<out T>(
    val response: String?,
    @SerializedName("data_list")
    @Expose
    val modelList: List<T>
)
