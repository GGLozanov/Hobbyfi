package com.example.hobbyfi.adapters.tag

import com.example.hobbyfi.models.Tag
import com.google.gson.*
import java.lang.reflect.Type

class TagTypeAdapter : JsonSerializer<Tag> {
    override fun serialize(
        src: Tag?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        if(src == null) {
            return null
        }

        val tagJsonObject = JsonObject()
        val tagPropertiesJson = JsonArray()
        val tagColourJson = JsonObject()

        tagColourJson.addProperty("colour", src.colour)
        tagPropertiesJson.add(tagColourJson)
        tagJsonObject.add(src.name, tagPropertiesJson)
        return tagJsonObject
    }
}