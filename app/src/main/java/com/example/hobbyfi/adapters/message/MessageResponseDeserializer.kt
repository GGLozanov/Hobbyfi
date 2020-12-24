package com.example.hobbyfi.adapters.message

import com.example.hobbyfi.adapters.BaseJsonDeserializer
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.User
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.fromJson
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MessageResponseDeserializer : BaseJsonDeserializer<CacheResponse<Message>>() {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CacheResponse<Message> {
        outerJsonObject = json.asJsonObject.getAsJsonObject(Constants.DATA_LIST)

        val entity = Message(
            deserializeJSONField(Constants.ID, DeserializeOption.AS_LONG) as Long,
            deserializeJSONField(Constants.MESSAGE, DeserializeOption.AS_STRING) as String?,
            deserializeJSONField(Constants.CREATE_TIME, DeserializeOption.AS_STRING) as String,
            deserializeJSONField(Constants.USER_SENT_ID, DeserializeOption.AS_LONG) as Long,
            deserializeJSONField(Constants.CHATROOM_SENT_ID, DeserializeOption.AS_LONG) as Long
        )

        var response = deserializeJSONField(
            Constants.RESPONSE,
            DeserializeOption.AS_STRING,
            json.asJsonObject
        ) as String? // response may not always be contained in API return JSON (like in getUsers request)

        if (response == null) {
            response = Constants.SUCCESS_RESPONSE
        }

        return CacheResponse(response, entity)
    }
}