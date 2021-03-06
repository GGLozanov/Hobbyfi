package com.example.hobbyfi.responses

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CacheResponse<out T>(
    val response: String?,
    @SerializedName("data")
    @Expose
    val model: T
) // always has at least one room entity associated with its response