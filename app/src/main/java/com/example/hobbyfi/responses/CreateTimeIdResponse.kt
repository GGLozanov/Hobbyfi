package com.example.hobbyfi.responses

import com.example.hobbyfi.shared.Constants
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

// lmao data class inheritance go brrr
data class CreateTimeIdResponse(
    val response: String?,
    @SerializedName("id")
    @Expose
    val id: Long,
    @SerializedName(Constants.CREATE_TIME)
    val createTime: String
)
