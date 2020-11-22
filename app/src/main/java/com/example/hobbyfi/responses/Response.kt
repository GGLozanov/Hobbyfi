package com.example.hobbyfi.responses

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

open class Response(
    @SerializedName("response")
    @Expose
    open val response: String?
)