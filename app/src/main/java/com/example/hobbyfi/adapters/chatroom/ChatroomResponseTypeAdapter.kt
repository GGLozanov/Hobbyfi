package com.example.hobbyfi.adapters.chatroom

import com.example.hobbyfi.responses.ChatroomResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

// will probably need this
class ChatroomResponseTypeAdapter : JsonDeserializer<ChatroomResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ChatroomResponse {
        TODO("Not yet implemented")
    }
}