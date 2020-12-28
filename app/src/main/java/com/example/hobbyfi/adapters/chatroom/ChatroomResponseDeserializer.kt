package com.example.hobbyfi.adapters.chatroom

import com.example.hobbyfi.adapters.BaseJsonDeserializer
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import java.lang.reflect.Type

class ChatroomResponseDeserializer : BaseJsonDeserializer<CacheResponse<Chatroom>>() {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CacheResponse<Chatroom> {
        outerJsonObject = json.asJsonObject.getAsJsonObject(Constants.DATA_LIST)

        val entity = Chatroom(
            deserializeJSONField(Constants.ID, DeserializeOption.AS_LONG) as Long,
            deserializeJSONField(Constants.NAME, DeserializeOption.AS_STRING) as String,
            deserializeJSONField(Constants.DESCRIPTION, DeserializeOption.AS_STRING) as String?,
            deserializeJSONField(Constants.PHOTO_URL, DeserializeOption.AS_STRING) as String?,
            Constants.tagJsonConverter
                .fromJson(deserializeJSONField(Constants.TAGS, DeserializeOption.AS_ARRAY) as JsonArray?),
            deserializeJSONField(Constants.OWNER_ID, DeserializeOption.AS_INT) as Long,
            deserializeJSONField(Constants.LAST_EVENT_ID, DeserializeOption.AS_LONG) as Long?
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