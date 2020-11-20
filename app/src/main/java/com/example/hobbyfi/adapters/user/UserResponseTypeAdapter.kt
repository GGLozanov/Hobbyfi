package com.example.hobbyfi.adapters.user

import com.example.hobbyfi.responses.UserResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class UserResponseTypeAdapter : JsonDeserializer<UserResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): UserResponse {
        TODO("Not yet implemented")
    }
}