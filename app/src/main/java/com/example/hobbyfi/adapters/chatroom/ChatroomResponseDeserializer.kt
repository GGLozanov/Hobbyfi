package com.example.hobbyfi.adapters.chatroom

import com.example.hobbyfi.adapters.BaseJsonDeserializer
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.responses.CacheResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import java.lang.reflect.Type

// will probably need this
class ChatroomResponseDeserializer : BaseJsonDeserializer<CacheResponse<Chatroom>>() {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CacheResponse<Chatroom> {
        TODO("Not yet implemented")
    }
}