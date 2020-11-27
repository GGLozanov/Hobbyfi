package com.example.hobbyfi.adapters

import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject

abstract class BaseJsonDeserializer<T> : JsonDeserializer<T> {
    protected var jsonObject: JsonObject? = null

    protected enum class DeserializeOption {
        AS_STRING, AS_INT, AS_LONG, AS_ARRAY
    }

    // TODO: Bad function. Delete
    protected fun deserializeJSONField(field: String, flag: DeserializeOption): Any? {
        val jsonElement = jsonObject?.get(field)
        return if (jsonElement != null && !jsonElement.isJsonNull) {
            // banal/naive/whatever way to do it but oh well. . .
            when (flag) {
                DeserializeOption.AS_INT -> jsonElement.asInt
                DeserializeOption.AS_LONG -> jsonElement.asLong
                DeserializeOption.AS_STRING -> jsonElement.asString
                DeserializeOption.AS_ARRAY -> {
                    jsonElement.asJsonArray
                }
            }
        } else null
    }
}