package com.example.hobbyfi.adapters.tag

import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.Exception

class TagTypeAdapter : TypeAdapter<Tag>() {

    override fun write(out: JsonWriter?, value: Tag?) {
        if (value == null || out == null) {
            return
        }

        out.beginObject()
        out.name(Constants.name)
        out.value(value.name)
        out.name(Constants.colour)
        out.value(value.colour)
        out.name(Constants.isFromFacebook)
        out.value(value.isFromFacebook)
        out.endObject()
    }

    override fun read(reader: JsonReader?): Tag {
        if(reader == null) {
            throw Exception("Empty Json Reader in Tag Type Adapter!")
        }

        reader.beginObject()
        var tagName: String? = null
        var tagColour: String? = null
        var tagFacebook = false

        while(reader.hasNext()) {
            var token = reader.peek()

            if(token == JsonToken.NAME) {
                val name = reader.nextName()
                token = reader.peek() // if ugly response peek might might not be apt since there won't be either colour or name field
                if(name == Constants.colour) {
                    tagColour = reader.nextString()
                } else if(name == Constants.name) {
                    tagName = reader.nextString()
                } else if(name == Constants.isFromFacebook) {
                    tagFacebook = reader.nextBoolean()
                }
            }
        }

        reader.endObject()

        return Tag(tagName!!, tagColour!!, tagFacebook)
    }
}