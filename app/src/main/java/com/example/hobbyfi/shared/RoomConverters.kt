package com.example.hobbyfi.shared

import androidx.room.TypeConverter
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.models.Tag
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class RoomConverters {

    @TypeConverter
    fun fromRemoteKeyType(value: RemoteKeyType): String = value.name

    @TypeConverter
    fun toRemoteKeyType(value: String): RemoteKeyType = enumValueOf(value)

    @TypeConverter
    fun fromTagList(value: List<Tag>?): String {
        return Constants.tagJsonConverter.toJson(value)
    }

    @TypeConverter
    fun toTagList(value: String): List<Tag>? {
        return Constants.tagJsonConverter.fromJson<List<Tag>>(value)
    }
}