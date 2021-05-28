package com.example.hobbyfi.requests

import com.example.hobbyfi.shared.Constants
import com.google.gson.annotations.SerializedName
import retrofit2.http.Field

data class FetchRegisterTokenRequest(
    val email: String?,
    val username: String,
    val password: String?,
    val description: String?,
    @SerializedName(Constants.TAGS + "[]")
    val tags: String?
)
