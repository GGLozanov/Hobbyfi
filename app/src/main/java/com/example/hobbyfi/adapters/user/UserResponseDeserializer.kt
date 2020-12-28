package com.example.hobbyfi.adapters.user

import android.util.Log
import com.example.hobbyfi.adapters.BaseJsonDeserializer
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.google.gson.*
import java.lang.reflect.Type
import com.example.hobbyfi.models.User
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.shared.fromJson

class UserResponseDeserializer(private val forList: Boolean = false) : BaseJsonDeserializer<CacheResponse<User>>() {

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CacheResponse<User> {
        outerJsonObject = json.asJsonObject.getAsJsonObject(if(forList) Constants.DATA_LIST else Constants.DATA)

        val entity = User(
            deserializeJSONField(Constants.ID, DeserializeOption.AS_LONG) as Long,
            deserializeJSONField(Constants.EMAIL, DeserializeOption.AS_STRING) as String?,
            deserializeJSONField(Constants.USERNAME, DeserializeOption.AS_STRING) as String,
            deserializeJSONField(Constants.DESCRIPTION, DeserializeOption.AS_STRING) as String?,
            deserializeJSONField(Constants.PHOTO_URL, DeserializeOption.AS_STRING) as String?,
            Constants.tagJsonConverter
                .fromJson(deserializeJSONField(Constants.TAGS, DeserializeOption.AS_ARRAY) as JsonArray?),
            deserializeJSONField(Constants.CHATROOM_ID, DeserializeOption.AS_LONG) as Long?
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