package com.example.hobbyfi.adapters.user

import com.example.hobbyfi.adapters.BaseJsonDeserializer
import com.example.hobbyfi.shared.Constants
import com.google.gson.*
import java.lang.reflect.Type
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.shared.fromJson

class UserResponseDeserializer : BaseJsonDeserializer<CacheResponse<User>>() {

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CacheResponse<User> {
        outerJsonObject = json.asJsonObject.getAsJsonObject(Constants.DATA)

        val entity = User(
            deserializeJSONField(Constants.ID, DeserializeOption.AS_LONG) as Long,
            deserializeJSONField(Constants.EMAIL, DeserializeOption.AS_STRING) as String?,
            deserializeJSONField(Constants.USERNAME, DeserializeOption.AS_STRING) as String,
            deserializeJSONField(Constants.DESCRIPTION, DeserializeOption.AS_STRING) as String?,
            deserializeJSONField(Constants.PHOTO_URL, DeserializeOption.AS_STRING) as String?,
            Constants.jsonConverter
                .fromJson(deserializeJSONField(Constants.TAGS, DeserializeOption.AS_ARRAY) as JsonArray?),
            Constants.jsonConverter
                .fromJson(deserializeJSONField(Constants.CHATROOM_IDS, DeserializeOption.AS_ARRAY) as JsonArray?),
            Constants.jsonConverter
                .fromJson(deserializeJSONField(Constants.ALLOWED_PUSH_CHATROOM_IDS, DeserializeOption.AS_ARRAY) as JsonArray?)
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