package com.example.hobbyfi.responses

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class IdResponse(
    val response: String?,
    @SerializedName("id")
    @Expose
    val id: Long
)