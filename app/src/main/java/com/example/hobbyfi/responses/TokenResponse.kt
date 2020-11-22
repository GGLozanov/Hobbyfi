package com.example.hobbyfi.responses

import androidx.room.Ignore
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class TokenResponse(
    response: String?,
    @SerializedName("jwt")
    @Expose
    val jwt: String?,
    @SerializedName("refresh_jwt")
    @Expose
    val refreshJwt: String?
) : Response(response)