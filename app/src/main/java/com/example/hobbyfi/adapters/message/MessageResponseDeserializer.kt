package com.example.hobbyfi.adapters.message

import com.example.hobbyfi.adapters.BaseJsonDeserializer
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.responses.CacheListResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MessageResponseDeserializer : BaseJsonDeserializer<CacheListResponse<Chatroom>>() {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CacheListResponse<Chatroom> {
        TODO("Not yet implemented")
    }
}