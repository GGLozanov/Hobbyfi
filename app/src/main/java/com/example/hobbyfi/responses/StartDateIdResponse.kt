package com.example.hobbyfi.responses

import com.example.hobbyfi.shared.Constants
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class StartDateIdResponse(
    val response: String?,
    @SerializedName("id")
    @Expose
    val id: Long,
    @SerializedName(Constants.START_DATE)
    val startDate: String
)