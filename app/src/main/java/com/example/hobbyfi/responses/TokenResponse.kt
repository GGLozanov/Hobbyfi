package com.example.hobbyfi.responses

import androidx.room.Ignore
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @Expose
    val response: String?,
    @Expose
    val jwt: String?,
    @SerializedName("refresh_jwt")
    @Expose
    val refreshJwt: String?
)