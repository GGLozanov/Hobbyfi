package com.example.hobbyfi.adapters.message

import com.example.hobbyfi.responses.MessageResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MessageResponseTypeAdapter : JsonDeserializer<MessageResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MessageResponse {
        TODO("Not yet implemented")
    }
}