package com.example.hobbyfi.responses

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("response")
    @Expose
    override val response: String?,
    @SerializedName("jwt")
    @Expose
    val jwt: String?,
    @SerializedName("refresh_jwt")
    @Expose
    val refreshJwt: String?
) : Response